package com.snl.services.throttle

import scala.collection.mutable
import scala.util.control.NonFatal
import scala.concurrent._

import akka.actor._
import kafka.serializer.{Decoder, StringDecoder}
import org.apache.spark.{ Logging => _, _ }
import org.apache.spark.SparkContext._
import org.apache.spark.streaming._
import org.apache.spark.streaming.StreamingContext._
import org.apache.spark.storage._
import org.apache.spark.streaming.kafka._
import grizzled.slf4j._

// use couchbase for state writing
import com.snl.services.throttle.CouchbaseRequestStateWriter._

/**
 * The main actor for the throttle application
 * 
 * TODOS:
 * 1) figure out how to use application.conf and pass system parms
 * 2) make it run on yarn
 * 	a) at all
 *  b) support override of kafka/couchbase connect strings
 * 3) handle errors in bucket upsert, fail agent and force restart?
 * 4) configure bucket to update frequently or adjust the stale settings on neg responses?
 * 5) in web service, accept multiple requestGroup,hits pairs (document how to create view and configure properly)
 */
class Throttle extends Actor with Logging {

  import context._
  
  /**
   * Pull in the configuration
   */
  private val config = Configuration(system)
  
  /**
   * The spark context
   */
  private lazy val sparkContext : SparkContext =  {
    
    // the spark configuration
    val conf = new SparkConf()
      .setAppName(config.appName)
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .set("spark.streaming.receiver.writeAheadLogs.enable", "true")
      
    // if supplied, set the spark master
    config.sparkMaster match {
      case Some(master) => conf.setMaster(master)
      case None => {}
    }
      
    // register classes that will need to be serialized
    conf.registerKryoClasses( Array(classOf[Configuration]))
    	
    // create the spark context
    new SparkContext( conf )
    
  }
  
  /**
   * The spark streaming context
   */
  private lazy val streamingContext : StreamingContext = StreamingContext.getOrCreate(config.sparkCheckpoints, () => {
    
    // create the streaming context
    val context = new StreamingContext( sparkContext, config.sparkBatchInterval )
    context.checkpoint(config.sparkCheckpoints)
    context
    
  })
  
  /**
   * Called to start up spark streaming
   */
  override def preStart {

    // the broadcast config, use this in any tasks where config needs to be accessed
	val taskConfig = sparkContext.broadcast(config)
	
	// some accumulators
	val totalHits = sparkContext.accumulator(0L, "Total hits" )
    
	// create the input stream of requests from kafka. note that we have configured write-ahead to be enabled in the 
	// spark configuration above, to achieve exactly once semantics, and have disabled replication for this stream
	// per the recommendation in the docs, see https://spark.apache.org/docs/latest/streaming-programming-guide.html#fault-tolerance-semantics
	val rawRequests = streamingContext.union(( 1 to config.requestsTopicReceiverCount).map( i => 
	  KafkaUtils.createStream[String,String,StringDecoder,StringDecoder]( 
	    streamingContext,
	    Map( 
	        "zookeeper.connect"-> config.kafkaZookeeperConnect,
	        "group.id" -> config.appName 
	    ),
	    Map( 
	        config.requestsTopic -> config.requestsTopicThreadsPerReceiver
	    ),
	    StorageLevel.MEMORY_AND_DISK_SER)))
	    
	// parse the requests and throw out any that are too old, e.g. that have aged out of the window interval 
	// the incoming request is is key, value where the key is the trackingKey and the value is (time, hits)
	// the result is a DStream of ( key (time, hits))
	val requests = rawRequests.map( r => {
	  
	  val key = r._1
 	  val parts = r._2.split(",")
	  val time = parts(0).toLong
	  val hits = parts(1).toLong
	  ( key, ( time, hits ))
	  
	}).transform( (rdd, time ) => {
	  
	  val threshold = time.milliseconds - taskConfig.value.requestsWindowInterval.toMillis
	  rdd.filter( r => r._2._1 > threshold ).mapValues( _._2)
	  
	})
	
	// send through counts of zero in order to make sure that things get cleared out when requests stop, use the window size * 2 to make sure
	// that we always get trailing zeros to force the totals to get reduced to zero
	val zeroCounts = requests.groupByKeyAndWindow(
	    config.requestsWindowInterval * 2,						// kgw try window interval + slide interval? 
	    config.requestsSlideInterval).map( r => ( r._1, 0L ))

	// count up the values over the trailing window
	val counts = requests.union(zeroCounts).reduceByKeyAndWindow(
	    (a: Long, b: Long) => a + b,
	    config.requestsWindowInterval, 
	    config.requestsSlideInterval, 
	    config.requestsReducePartitionCount )
	
	// process each rdd ..
	counts.foreachRDD( rdd => {

	  // process each record
	  rdd.foreach( r => {
	    
	    // write the state
	    writeState( r._1, r._2, taskConfig.value )
	    
	  })
	  
	})
	
	
	// kick off the processing
	self ! Throttle.StartMessage
	
  }
  
  /**
   * Message handler
   */
  def receive = {
    
    case Throttle.StartMessage => {
    
      // start the streaming context
      streamingContext.start()
      
      future {
      
        try {
          
	        // wait for termination
	        streamingContext.awaitTermination()
	        
	        // normal shutdown
	        logger.info("Streaming context terminated normally")
          
        }
        catch {
          
          case NonFatal(cause) => {
            
            // kill the actor
            logger.error( "Streaming context terminated abnormally: %s".format( cause.getMessage()))
            self ! Kill
            
          }
          
        }
        
      }
      
    }
    
  }
  
  /**
   * Stop handler
   */
  override def postStop {
    
    // stop the streaming context (including spark context)
    streamingContext.stop(true)
    
  }
  
}

object Throttle {
  
  /**
   * The message that starts the processing
   */
  case object StartMessage

  /**
   * Actor properties
   */
  def props() = Props( classOf[Throttle])
}