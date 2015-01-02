package com.snl.services.throttle

import akka.actor._
import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.spark.storage._
import org.apache.spark.streaming.kafka._

/**
 * The main actor for the throttle application
 */
class Main extends Actor {

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
      .setMaster(config.sparkMaster)
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .set("spark.streaming.receiver.writeAheadLogs.enable", "true")
      
    // register classes that will need to be serialized
    conf.registerKryoClasses( Array(classOf[Configuration]))
    	
    // create the spark context
    new SparkContext( conf )
    
  }
  
  /**
   * The spark streaming context
   */
  private lazy val streamingContext : StreamingContext = {
    
    // create the streaming context
    val context = new StreamingContext( sparkContext, config.sparkBatchInterval )
    context.checkpoint(config.sparkCheckpoints)
    context
    
  }
  
  /**
   * Called to start up spark streaming
   */
  override def preStart {

    // the broadcast config, use this in any tasks where config needs to be accessed
	val taskConfig = sparkContext.broadcast(config)
    
	// create the input stream of requests from kafka. note that we have configured write-ahead to be enabled in the 
	// spark configuration above, to achieve exactly once semantics, and have disabled replication for this stream
	// per the recommendation in the docs, see https://spark.apache.org/docs/latest/streaming-programming-guide.html#fault-tolerance-semantics
	val rawRequests = streamingContext.union(( 1 to config.requestsTopicReceiverCount).map( i => KafkaUtils.createStream( 
	    streamingContext,
	    Map( 
	        "zookeeper.connect"-> config.kafkaZookeeperConnect,
	        "group.id" -> config.appName
	    ),
	    Map( 
	        config.requestsTopic -> config.requestsTopicThreadsPerReceiver
	    ),
	    StorageLevel.MEMORY_AND_DISK_SER)))
	    
	// parse the requests
	val requests = rawRequests
    
	// kick off the processing
	self ! Main.StartMessage
	
  }
  
  /**
   * Message handler
   */
  def receive = {
    
    case Main.StartMessage => {
    
      // kgw add in awaitTermination handler
      streamingContext.start()
      
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

object Main {
  
  /**
   * The message that starts the processing
   */
  case object StartMessage
  
}