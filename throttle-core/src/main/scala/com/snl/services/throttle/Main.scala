package com.snl.services.throttle

import scala.collection.mutable
import scala.util.control.NonFatal
import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor._
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import grizzled.slf4j._

// use couchbase for state writing
import com.snl.services.throttle.CouchbaseRequestStateWriter._

/**
 * The main actor for the throttle application
 */
class Main extends Actor with Logging {

  import context._
  
  /**
   * Pull in the configuration
   */
  private val config = Configuration(system)
  
  /**
   * The throttle actor
   */
  private val throttle = actorOf(Throttle.props())
  
  /**
   * The supervisor strategy
   */
  override val supervisorStrategy = OneForOneStrategy( maxNrOfRetries = config.retryMaxRetries, withinTimeRange = config.retryTimeInterval) {
    case _: Exception                => Restart
  } 
  
  /**
   * Message handler
   */
  def receive = {
    case _ => {}
  }
  
}

