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

import org.apache.spark.sql.types.{DataType, StructType}
import org.scalatest.funsuite.AnyFunSuite
import za.co.absa.spark.commons.test.SparkTestBase
import za.co.absa.standardization.types.{Defaults, GlobalDefaults}
import za.co.absa.standardization.udf.UDFLibrary
import za.co.absa.standardization._

class SampleDataSuite extends AnyFunSuite with SparkTestBase with LoggerTestBase {
  private implicit val defaults: Defaults = GlobalDefaults

  test("Simple Example Test") {
    import spark.implicits._
    val data = spark.createDataFrame(TestSamples.data1)

    logDataFrameContent(data)

    implicit val udfLib: UDFLibrary = new UDFLibrary()

    val sourceFile = FileReader.readFileAsString("src/test/resources/data/data1Schema.json")
    val schema = DataType.fromJson(sourceFile).asInstanceOf[StructType]
    val std = Standardization.standardize(data, schema)
    logDataFrameContent(std)
    val stdList = std.as[StdEmployee].collect.sortBy(_.name).toList
    val exp = TestSamples.resData.sortBy(_.name)

    assertResult(exp)(stdList)

  }

}
