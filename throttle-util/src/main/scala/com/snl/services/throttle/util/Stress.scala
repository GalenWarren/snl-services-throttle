package com.snl.services.throttle.util

import java.util.{Properties,UUID}
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
   * Generate some random ids
   */
  private val modelIds = ( 1 to config.stressUtilKeyCount).map( i => UUID.randomUUID().toString())
  
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
      
      for (modelId <- modelIds) {
        
    	  // generate a value of hits between 1 and max
    	  val hits = random.nextInt( config.stressUtilMaxHits ) + 1
    	  
    	  // get the current time
    	  val time = System.currentTimeMillis()
    	  
    	  // send the message
    	  producer.send( KeyedMessage( config.requestsTopic, modelId, modelId, "%s,%s".format( time.toString, hits.toString )))  
    	  logger.info( "Generated request with for model %s with %d hits".format( modelId, hits ))
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