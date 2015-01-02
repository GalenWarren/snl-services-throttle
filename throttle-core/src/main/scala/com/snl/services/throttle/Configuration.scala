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
   * The implicit ask timeout
   */
  implicit val askTimeout = Configuration.getDuration( config, "snl.askTimeout")

  /**
   * The name of the application
   */
  val appName = "SNL Throttling Service v1"
    
  /**
   * The spark master
   */
  val sparkMaster = config.getString("snl.services.throttle.spark.master")
  
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
   * The zookeeper connect string for kafka
   */
  val kafkaZookeeperConnect = config.getString("snl.services.throttle.kafka.zookeeperConnect")
  
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