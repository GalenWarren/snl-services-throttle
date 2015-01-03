package com.snl.services.throttle

import akka.actor._

/**
 * A trait for a configured actor
 */
trait Configured extends Actor {

  /**
   * Pull in the configuration
   */
  protected lazy val config = Configuration(context.system)
  
  
}

