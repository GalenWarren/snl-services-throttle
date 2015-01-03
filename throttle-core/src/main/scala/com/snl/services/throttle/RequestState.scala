package com.snl.services.throttle

import java.util.UUID

import com.couchbase.client.java.document._
import com.couchbase.client.java.document.json._

/**
 * Class that represents the request state for a key and site
 */
case class RequestState( key: String, site: String, count: Long ) {

  /**
   * Generates a json document appropriate for couchbase
   */
  def toJsonDocument( config: Configuration ) = {
    
//    JsonDocument.create(UUID.randomUUID().toString(), config.requestsWindowInterval.toSeconds.toInt, JsonObject.empty()
    JsonDocument.create( "%s:%s".format( key, site ), JsonObject.empty().put( "count", count ))
  }
  
}