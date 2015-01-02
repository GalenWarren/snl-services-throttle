package com.snl.services

import scala.language.implicitConversions

package object throttle {
  
  /**
   * An implicit conversion from scala duration to spark duration
   */
  implicit def ScalaDuration2SparkDuration( duration: scala.concurrent.duration.Duration ) =
    org.apache.spark.streaming.Duration( duration.toMillis)

}