/*
 * Copyright 2021 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.standardization.time

import java.util.TimeZone

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.log4j.{LogManager, Logger}
import org.apache.spark.sql.SparkSession

/**
  * Sets the system time zone per application configuration, recommended value being UTC
   */
object TimeZoneNormalizer {
  private val log: Logger = LogManager.getLogger(this.getClass)
  private val generalConfig: Config = ConfigFactory.load()
  val timeZone: String = if (generalConfig.hasPath("timezone")){
    generalConfig.getString("timezone")
  } else {
    val default = "UTC"
    log.warn(s"No time zone (timezone) setting found. Setting to default, which is $default.")
    default
  }

  def normalizeJVMTimeZone(): Unit = {
    TimeZone.setDefault(TimeZone.getTimeZone(timeZone))
    log.debug(s"JVM time zone set to $timeZone")
  }

  def normalizeSessionTimeZone(spark: SparkSession): Unit = {
    spark.conf.set("spark.sql.session.timeZone", timeZone)
    log.debug(s"Spark session ${spark.sparkContext.applicationId} time zone of name ${spark.sparkContext.appName} set to $timeZone")
  }

  def normalizeAll(spark: SparkSession): Unit = {
    normalizeJVMTimeZone()
    normalizeSessionTimeZone(spark)
  }

}
