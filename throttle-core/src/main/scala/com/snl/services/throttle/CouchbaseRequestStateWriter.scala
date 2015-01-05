package com.snl.services.throttle

import java.net.URLEncoder
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
  private var bucketOption: Option[Bucket] = None
  
  /**
   * The couchbase client object
   */
  private def getBucket( config: Configuration ) : Bucket = {
    
    bucketOption match {
      
      case Some(b) => b
      case None => synchronized {

        bucketOption match {
          case Some(b) => b
          case None => {

	        // create the cluster
	        val cluster = CouchbaseCluster.create( config.couchbaseNodes.split(",") :_*)
	        
	        // create the bucket
	        val bucket = cluster.openBucket( config.requestsBucket)
	        bucketOption = Some(bucket)
	        bucket
            
          }
          
        }
        
      }
      
    }
    
  }
  
  /**
   * Writes out the state
   */
  def writeState( key: String, count: Long, config: Configuration ) {
    
    // access the cluster
    val bucket = getBucket( config )
    
    // generate the bucket key, this will look something like 12345:API:HQ, where the 12345:API
    // is what came in as the key and we add the site here
    val bucketKey = List( key, URLEncoder.encode(config.site, "utf8")).mkString(":")
    
    // either delete or upsert depending on the count. if we insert, give it an expiry of 2x the trailing interval
	count match {
      case 0 => bucket.remove( bucketKey )
      case _ => bucket.upsert(JsonDocument.create( 
          bucketKey, 
          config.requestsWindowInterval.toSeconds.toInt, 
          JsonObject.empty().put( "count", count )))
    }
    
  }
  
}