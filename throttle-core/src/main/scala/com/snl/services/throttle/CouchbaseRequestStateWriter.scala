package com.snl.services.throttle

import scala.collection.JavaConversions._
import scala.util.control._

import com.couchbase.client.java._
import com.couchbase.client.java.document._
import com.couchbase.client.java.document.json._
import rx.lang.scala._
import rx.lang.scala.JavaConversions._
import grizzled.slf4j._

/**
 * Request state writer for couchbase
 */
object CouchbaseRequestStateWriter extends RequestStateWriter with Logging {

  /**
   * The bucket
   */
  private var bucketOption: Option[AsyncBucket] = None
  
  /**
   * The couchbase client object
   */
  private def getBucket( config: Configuration ) : AsyncBucket = synchronized {
    
    bucketOption match {
      
      case Some(b) => b
      case None => {
        
        // create the cluster
        val cluster = CouchbaseCluster.create( config.couchbaseNodes.split(",") :_*)
        
        // create the bucket
        val bucket = cluster.openBucket( config.requestsBucket).async()
        bucketOption = Some(bucket)
        bucket
        
      }
      
    }
    
  }
  
  /**
   * Writes out the state
   */
  def writeState( key: String, count: Long, config: Configuration ) {
    
    // access the cluster
    val bucket = getBucket( config )
    
    // generate the bucket key
    val bucketKey = List( key, config.site ).mkString(":")
    
    // either delete or upsert depending on the count. if we insert, give it an expiry of 2x the trailing interval
	val observable : Observable[JsonDocument] = count match {
      case 0 => bucket.remove( bucketKey )
      case _ => bucket.upsert(JsonDocument.create( bucketKey, JsonObject.empty().put( "count", count ), config.requestsWindowInterval.toSeconds ))
    }
    
    // subscribe to results
    observable.subscribe( new Observer[JsonDocument] {
      override def onError( t: Throwable ) {
        logger.error( "Failed to store document for key %s: %s".format( key, t.getMessage()))
      }
    })
    
  }
  
}