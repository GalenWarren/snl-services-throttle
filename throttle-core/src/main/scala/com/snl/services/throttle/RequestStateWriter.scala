package com.snl.services.throttle

/**
 * Trait for a state writer
 */
trait RequestStateWriter {

  /**
   * Writes out the state
   */
  def writeState( key: String, count: Long, config: Configuration ) : Unit
  
}