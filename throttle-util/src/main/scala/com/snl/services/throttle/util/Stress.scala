package com.snl.services.throttle.util

import java.util.{Properties,UUID}
import java.net.URLEncoder
import scala.util.Random
import scala.concurrent.duration.Duration
import akka.actor._
import kafka.producer._
import grizzled.slf4j._
import org.apache.log4j.BasicConfigurator

/**
 * A stress utility
 */
class Stress extends Actor with Logging {

  import context._
  
  /**
   * Pull in the configuration
   */
  private val config = Configuration(system)
   
  /**
   * The kafka producer
   */
  lazy val producer : Producer[String,String] = {
    
    // fill in the properties
    val props = new Properties()
    props.put("metadata.broker.list", config.kafkaMetadataBrokerList)
    props.put("serializer.class", "kafka.serializer.StringEncoder");
    props.put("partitioner.class", "kafka.producer.DefaultPartitioner");
    props.put("request.required.acks", "1");
    
    // create the producer
    new Producer[String,String]( new ProducerConfig( props ))
    
  }

  /**
   * The keys to use
   */
  private val keys = ( 1 to config.stressUtilKeyCount).map( i => "key%d".format( i ))
  
  /**
   * The request groups to use
   */
  private val requestGroups = (1 to config.requestGroupCount).map( i => "requestGroup%d".format(i))
  
  /**
   * Random number generator
   */
  private val random = new Random()
  
  /**
   * Startup
   */
  override def preStart {
    
    // default logging config (to console)
	BasicConfigurator.configure()    
    
    // start up the scheduler 
    system.scheduler.schedule( Duration.Zero, config.stressUtilPeriod, self, Stress.GenerateMessage)
    
  }
  
  /**
   * Message handler
   */
  def receive = {
    
    case Stress.GenerateMessage => {

      for (key <- keys; requestGroup <- requestGroups) {
        
    	  // generate a value of hits between 1 and max
    	  val hits = random.nextInt( config.stressUtilMaxHits ) + 1

		  // use the current time
    	  val time = System.currentTimeMillis()

    	  // construct and send the message
    	  val messageKey = List( key, requestGroup ).map( s => URLEncoder.encode(s, "utf8")).mkString(":")
    	  val message = List( time.toString, hits.toString ).mkString(",")
    	  producer.send( KeyedMessage( config.requestsTopic, messageKey, messageKey, message ))  
    	  logger.info( "Generated request with for key %s and request group %s with %d hits".format( key, requestGroup, hits ))
      }
      
    }
    
  }
  
}

object Stress {
  
  /**
   * Generate hits 
   */
  case object GenerateMessage
  
}