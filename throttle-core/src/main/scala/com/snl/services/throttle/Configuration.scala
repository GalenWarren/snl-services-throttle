package com.snl.services.throttle

import akka.actor._
import akka.util._
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import com.typesafe.config._

/**
 * The configuration extension
 */
class Configuration ( config: Config ) extends Extension {

  /**
   * The name of the application
   */
  val appName = config.getString("snl.services.throttle.appName")
    
  /**
   * The name of the site
   */
  val site = config.getString("snl.services.throttle.site")
  
  /**
   * The spark master, this is optional as in production it is usually supplied via spark-submit
   */
  val sparkMaster : Option[String] = {
    val path = "snl.services.throttle.spark.master"
    config.hasPath(path) match {
      case true => Some(config.getString(path))
      case false => None
    }
  } 
  
  /**
   * The checkpoint location
   */
  val sparkCheckpoints = config.getString("snl.services.throttle.spark.checkpoints")
  
  /**
   * The spark batch interval
   */
  val sparkBatchInterval = Configuration.getDuration( config, "snl.services.throttle.spark.batchInterval")
  
  /**
   * The name of the topic for requests
   */
  val requestsTopic = config.getString("snl.services.throttle.requestsTopic")
  
  /**
   * The number of receivers for topics (corresponds to number of workers running a receiver)
   */
  val requestsTopicReceiverCount = config.getInt("snl.services.throttle.requestsTopicReceiverCount")

  /**
   * The number of threads per receiver
   */
  val requestsTopicThreadsPerReceiver = config.getInt("snl.services.throttle.requestsTopicThreadsPerReceiver")

  /**
   * The requests slide interval
   */
  val requestsSlideInterval = Configuration.getDuration( config, "snl.services.throttle.requestsSlideInterval")
  
  /**
   * The requests window interval
   */
  val requestsWindowInterval = Configuration.getDuration( config, "snl.services.throttle.requestsWindowInterval")
  
  /**
   * The requests bucket
   */
  val requestsBucket = config.getString( "snl.services.throttle.requestsBucket")
  
  /**
   * The partition count for the requests reduce operation
   */
  val requestsReducePartitionCount = config.getInt( "snl.services.throttle.requestsReducePartitionCount")
  
  /**
   * The zookeeper connect string for kafka
   */
  val kafkaZookeeperConnect = config.getString("snl.services.throttle.kafka.zookeeperConnect")

  /**
   * The couchbase nodes
   */
  val couchbaseNodes = config.getString("snl.services.throttle.couchbase.nodes")
  
  /**
   * The max number of times we can fail in the retry time interval
   */
  val retryMaxRetries = config.getInt( "snl.services.throttle.retry.maxRetries")
  
  /**
   * The retry time interval
   */
  val retryTimeInterval = Configuration.getDuration( config, "snl.services.throttle.retry.timeInterval")
  
}

object Configuration extends ExtensionId[Configuration] with ExtensionIdProvider {

  /**
   * Lookup value for this extension
   */
  override def lookup = Configuration
  
  /**
   * Create the extension
   */
  override def createExtension( system: ExtendedActorSystem ) = new Configuration( system.settings.config )
  
  /**
   * Helper to get a duration from a config file
   */
  def getDuration( config: Config, name: String, unit: TimeUnit = TimeUnit.MILLISECONDS ) = Duration( config.getDuration(name, unit), unit )
}