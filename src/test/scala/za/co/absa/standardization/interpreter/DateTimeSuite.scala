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

package za.co.absa.standardization.interpreter

import java.sql.{Date, Timestamp}

import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Dataset, Row}
import org.scalatest.funsuite.AnyFunSuite
import za.co.absa.spark.commons.test.SparkTestBase
import za.co.absa.standardization.stages.SchemaChecker
import za.co.absa.standardization.types.{Defaults, GlobalDefaults}
import za.co.absa.standardization.udf.UDFLibrary
import za.co.absa.standardization.validation.field.FieldValidationIssue
import za.co.absa.standardization._


class DateTimeSuite extends AnyFunSuite with SparkTestBase with LoggerTestBase {
  import spark.implicits._

  private implicit val defaults: Defaults = GlobalDefaults

  lazy val data: DataFrame = spark.createDataFrame(TestSamples.dateSamples)
  lazy val schemaWrong: StructType = DataType
    .fromJson(FileReader.readFileAsString("src/test/resources/data/dateTimestampSchemaWrong.json"))
    .asInstanceOf[StructType]
  lazy val schemaOk: StructType = DataType
    .fromJson(FileReader.readFileAsString("src/test/resources/data/dateTimestampSchemaOk.json"))
    .asInstanceOf[StructType]

  private implicit val udfLib: UDFLibrary = new UDFLibrary()

  test("Validation should return critical errors") {
    logger.debug(data.schema.prettyJson)
    val validationErrors = SchemaValidator.validateSchema(schemaWrong)
    val exp = List(
      FieldValidationIssue("dateSampleWrong1", "DD-MM-yyyy", List(
        ValidationWarning("No day placeholder 'dd' found."),
        ValidationWarning("Rarely used DayOfYear placeholder 'D' found. Possibly DayOfMonth 'd' intended."))),
      FieldValidationIssue("dateSampleWrong2", "Dy", List(
        ValidationWarning("No day placeholder 'dd' found."),
        ValidationWarning("Rarely used DayOfYear placeholder 'D' found. Possibly DayOfMonth 'd' intended."),
        ValidationWarning("No month placeholder 'MM' found."))),
      FieldValidationIssue("dateSampleWrong3", "rrr", List(
        ValidationError("Illegal pattern character 'r'"))),
      FieldValidationIssue("timestampSampleWrong1", "yyyyMMddTHHmmss", List(
        ValidationError("Illegal pattern character 'T'"))),
      FieldValidationIssue("timestampSampleWrong3", "yyyy-MM-dd", List(
        ValidationWarning("No hour placeholder 'HH' found."),
        ValidationWarning("No minute placeholder 'mm' found."),
        ValidationWarning("No second placeholder 'ss' found."))),
      FieldValidationIssue("timestampNullDefaultWrong", "", List(
        ValidationError("null is not a valid value for field 'timestampNullDefaultWrong'")))
    )
    assert(validationErrors == exp)
  }

  test("Validation for this data should return critical errors") {
    val errors = SchemaChecker.validateSchemaAndLog(schemaWrong)
    assert(errors._1.nonEmpty)
  }

  test("Date Time Standardization Example Test should throw an exception") {
    intercept[ValidationException] {
      Standardization.standardize(data, schemaWrong)
    }
  }

  test("Date Time Standardization Example with fixed schema should work") {
    val date0 = new Date(0)
    val ts = Timestamp.valueOf("2017-10-20 08:11:31")
    val ts0 = new Timestamp(0)
    val exp = List((
      1L,
      Date.valueOf("2017-10-20"),
      Date.valueOf("2017-10-20"),
      Date.valueOf("2017-12-29"),
      date0,
      date0,
      null,
      ts, ts, ts, null, ts0, ts0,
      List(
        ErrorMessage.stdCastErr("dateSampleWrong1","10-20-2017"),
        ErrorMessage.stdCastErr("dateSampleWrong2","201711"),
        ErrorMessage.stdCastErr("dateSampleWrong3",""),
        ErrorMessage.stdCastErr("timestampSampleWrong1", "20171020T081131"),
        ErrorMessage.stdCastErr("timestampSampleWrong2", "2017-10-20t081131"),
        ErrorMessage.stdCastErr("timestampSampleWrong3", "2017-10-20")
      )
    ))
    val std: Dataset[Row] = Standardization.standardize(data, schemaOk)
    logDataFrameContent(std)
    assertResult(exp)(std.as[Tuple14[Long, Date, Date, Date, Date, Date, Date, Timestamp, Timestamp, Timestamp, Timestamp, Timestamp,Timestamp, Seq[ErrorMessage]]].collect().toList)
  }
}
