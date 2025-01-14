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

package za.co.absa.standardization.types

import java.util.TimeZone

import org.apache.spark.sql.types.DataType
import za.co.absa.standardization.ConfigReader
import za.co.absa.standardization.numeric.DecimalSymbols
import za.co.absa.standardization.types.DefaultsByFormat._

import scala.util.Try

class DefaultsByFormat(formatName: String,
                       globalDefaults: Defaults = GlobalDefaults,
                       private val config: ConfigReader = new ConfigReader()) extends  Defaults {

  /** A function which defines default values for primitive types */
  override def getDataTypeDefaultValue(dt: DataType): Any = globalDefaults.getDataTypeDefaultValue(dt)

  /** A function which defines default values for primitive types, allowing possible Null */
  override def getDataTypeDefaultValueWithNull(dt: DataType, nullable: Boolean): Try[Option[Any]] = {
    globalDefaults.getDataTypeDefaultValueWithNull(dt, nullable)
  }

  /** A function which defines default formats for primitive types */
  override def getStringPattern(dt: DataType): String = {
    globalDefaults.getStringPattern(dt)
  }

  override def getDefaultTimestampTimeZone: Option[String] = {
    defaultTimestampTimeZone.orElse(globalDefaults.getDefaultTimestampTimeZone)
  }

  override def getDefaultDateTimeZone: Option[String] = {
    defaultDateTimeZone.orElse(globalDefaults.getDefaultDateTimeZone)
  }

  override def getDecimalSymbols: DecimalSymbols = globalDefaults.getDecimalSymbols

  private def readTimezone(path: String): Option[String] = {
    val result = config.getStringOption(path)
    result.foreach(tz =>
      if (!TimeZone.getAvailableIDs().contains(tz )) {
        throw new IllegalStateException(s"The setting '$tz' of '$path' is not recognized as known time zone")
      }
    )
    result
  }

  private def formatSpecificConfigurationName(configurationName: String): String = {
    configurationFullName(configurationName, formatName)
  }

  private def configurationFullName(base: String, suffix: String): String = {
    s"$base.$suffix"
  }

  private val defaultTimestampTimeZone: Option[String] =
    readTimezone(formatSpecificConfigurationName(TimestampTimeZoneKeyName))
      .orElse(readTimezone(configurationFullName(TimestampTimeZoneKeyName, DefaultKeyName)))
      .orElse(readTimezone(DefaultsByFormat.ObsoleteTimestampTimeZoneName))

  private val defaultDateTimeZone: Option[String] =
    readTimezone(formatSpecificConfigurationName(DateTimeZoneKeyName))
      .orElse(readTimezone(configurationFullName(DateTimeZoneKeyName, DefaultKeyName)))
      .orElse(readTimezone(DefaultsByFormat.ObsoleteDateTimeZoneName))
}

object DefaultsByFormat {
  private final val DefaultKeyName = "default"
  private final val ObsoleteTimestampTimeZoneName = "defaultTimestampTimeZone"
  private final val ObsoleteDateTimeZoneName = "defaultDateTimeZone"
  private final val TimestampTimeZoneKeyName = "standardization.defaultTimestampTimeZone"
  private final val DateTimeZoneKeyName = "standardization.defaultDateTimeZone"
}
