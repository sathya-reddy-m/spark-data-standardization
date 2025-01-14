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

package za.co.absa.standardization

import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.scalatest.funsuite.AnyFunSuite
import za.co.absa.spark.commons.implicits.DataFrameImplicits.DataFrameEnhancements
import za.co.absa.spark.commons.test.SparkTestBase
import za.co.absa.standardization.types.{Defaults, GlobalDefaults}
import za.co.absa.standardization.udf.UDFLibrary

class StandardizationCsvSuite extends AnyFunSuite with SparkTestBase {
  import spark.implicits._

  private implicit val udfLib: UDFLibrary = new UDFLibrary
  private implicit val defaults: Defaults = GlobalDefaults


  private val csvContent = spark.sparkContext.parallelize(
    """101,102,1,2019-05-04,2019-05-04
      |201,202,2,2019-05-05,2019-05-05
      |301,302,1,2019-05-06,2019-05-06
      |401,402,1,2019-05-07,2019-05-07
      |501,502,,2019-05-08,2019-05-08"""
      .stripMargin.lines.toList ).toDS()

  test("Test standardizing a CSV without special columns") {
    val schema: StructType = StructType(Seq(
      StructField("A1", IntegerType, nullable = true),
      StructField("A2", IntegerType, nullable = true),
      StructField("A3", IntegerType, nullable = true),
      StructField("A4", StringType, nullable = true,
        Metadata.fromJson("""{"pattern": "yyyy-MM-dd"}""")),
      StructField("A5", StringType, nullable = true)
    ))

    val schemaWithStringType: StructType = StructType(Seq(
      StructField("A1", StringType, nullable = true),
      StructField("A2", StringType, nullable = true),
      StructField("A3", StringType, nullable = true),
      StructField("A4", StringType, nullable = true),
      StructField("A5", StringType, nullable = true)
    ))

    val expectedOutput =
      """+---+---+----+----------+----------+------+
        ||A1 |A2 |A3  |A4        |A5        |errCol|
        |+---+---+----+----------+----------+------+
        ||101|102|1   |2019-05-04|2019-05-04|[]    |
        ||201|202|2   |2019-05-05|2019-05-05|[]    |
        ||301|302|1   |2019-05-06|2019-05-06|[]    |
        ||401|402|1   |2019-05-07|2019-05-07|[]    |
        ||501|502|null|2019-05-08|2019-05-08|[]    |
        |+---+---+----+----------+----------+------+
        |
        |""".stripMargin.replace("\r\n", "\n")

    val rawDataFrame = spark.read.option("header", false).schema(schemaWithStringType).csv(csvContent)
    val stdDf = Standardization.standardize(rawDataFrame, schema).cache()
    val actualOutput = stdDf.dataAsString(truncate = false)

    assert(actualOutput == expectedOutput)
  }

  test("Test standardizing a CSV with special columns when error column has wrong type") {
    val schema: StructType = StructType(Seq(
      StructField("A1", IntegerType, nullable = true),
      StructField(ErrorMessage.errorColumnName, IntegerType, nullable = true),
      StructField("enceladus_info_version", IntegerType, nullable = true),
      StructField("enceladus_info_date", DateType, nullable = true,
        Metadata.fromJson("""{"pattern": "yyyy-MM-dd"}""")),
      StructField("enceladus_info_date_string", StringType, nullable = true)
    ))

    val schemaStr: StructType = StructType(Seq(
      StructField("A1", StringType, nullable = true),
      StructField(ErrorMessage.errorColumnName, StringType, nullable = true),
      StructField("enceladus_info_version", StringType, nullable = true),
      StructField("enceladus_info_date", StringType, nullable = true),
      StructField("enceladus_info_date_string", StringType, nullable = true)
    ))

    val rawDataFrame = spark.read.option("header", false).schema(schemaStr).csv(csvContent)

    assertThrows[ValidationException] {
      Standardization.standardize(rawDataFrame, schema).cache()
    }
  }

  test("Test standardizing a CSV with special columns when error column has correct type") {
    val schema: StructType = StructType(Seq(
      StructField("A1", IntegerType, nullable = true),
      StructField("A2", IntegerType, nullable = true),
      StructField("enceladus_info_version", IntegerType, nullable = false),
      StructField("enceladus_info_date", DateType, nullable = true,
        Metadata.fromJson("""{"pattern": "yyyy-MM-dd"}""")),
      StructField("enceladus_info_date_string", StringType, nullable = true)
    ))

    val schemaStr: StructType = StructType(Seq(
      StructField("A1", StringType, nullable = true),
      StructField("A2", StringType, nullable = true),
      StructField("enceladus_info_version", StringType, nullable = true),
      StructField("enceladus_info_date", StringType, nullable = true),
      StructField("enceladus_info_date_string", StringType, nullable = true)
    ))

    val rawDataFrame = spark.read.option("header", false).schema(schemaStr).csv(csvContent)
      .withColumn(ErrorMessage.errorColumnName, typedLit(List[ErrorMessage]()))

    val stdDf = Standardization.standardize(rawDataFrame, schema).cache()
    val failedRecords = stdDf.filter(size(col(ErrorMessage.errorColumnName)) > 0).count

    assert(stdDf.schema.exists(field => field.name == ErrorMessage.errorColumnName))
    assert(failedRecords == 1)
  }

}
