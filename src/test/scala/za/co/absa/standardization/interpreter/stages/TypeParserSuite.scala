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

package za.co.absa.standardization.interpreter.stages

import org.apache.spark.sql.types._
import org.scalatest.funsuite.AnyFunSuite
import za.co.absa.spark.commons.test.SparkTestBase
import za.co.absa.standardization.stages.TypeParser
import za.co.absa.standardization.types.{Defaults, GlobalDefaults}
import za.co.absa.standardization.udf.UDFLibrary

class TypeParserSuite extends AnyFunSuite with SparkTestBase {

  private implicit val udfLib: UDFLibrary = new UDFLibrary
  private implicit val defaults: Defaults = GlobalDefaults

  test("Test standardize with sourcecolumn metadata") {
    val structFieldNoMetadata = StructField("a", StringType)
    val structFieldWithMetadataNotSourceColumn = StructField("b", StringType, nullable = false, new MetadataBuilder().putString("meta", "data").build)
    val structFieldWithMetadataSourceColumn = StructField("c", StringType, nullable = false, new MetadataBuilder().putString("sourcecolumn", "override_c").build)
    val schema = StructType(Array(structFieldNoMetadata, structFieldWithMetadataNotSourceColumn, structFieldWithMetadataSourceColumn))
    //Just Testing field name override
    val parseOutputStructFieldNoMetadata = TypeParser.standardize(structFieldNoMetadata, "path", schema)
    assertResult(true)(parseOutputStructFieldNoMetadata.stdCol.expr.toString().contains("path.a"))
    assertResult(false)(parseOutputStructFieldNoMetadata.stdCol.expr.toString().replaceAll("path.a", "").contains("path"))
    assertResult(true)(parseOutputStructFieldNoMetadata.errors.expr.toString().contains("path.a"))
    assertResult(false)(parseOutputStructFieldNoMetadata.errors.expr.toString().replaceAll("path.a", "").contains("path"))
    val parseOutputStructFieldWithMetadataNotSourceColumn = TypeParser.standardize(structFieldWithMetadataNotSourceColumn, "path", schema)
    assertResult(true)(parseOutputStructFieldWithMetadataNotSourceColumn.stdCol.expr.toString().contains("path.b"))
    assertResult(false)(parseOutputStructFieldWithMetadataNotSourceColumn.stdCol.expr.toString().replaceAll("path.b", "").contains("path"))
    assertResult(true)(parseOutputStructFieldWithMetadataNotSourceColumn.errors.expr.toString().contains("path.b"))
    assertResult(false)(parseOutputStructFieldWithMetadataNotSourceColumn.errors.expr.toString().replaceAll("path.b", "").contains("path"))
    val parseOutputStructFieldWithMetadataSourceColumn = TypeParser.standardize(structFieldWithMetadataSourceColumn, "path",schema)
    assertResult(false)(parseOutputStructFieldWithMetadataSourceColumn.stdCol.expr.toString().contains("path.c"))
    assertResult(true)(parseOutputStructFieldWithMetadataSourceColumn.stdCol.expr.toString().contains("path.override_c"))
    assertResult(false)(parseOutputStructFieldWithMetadataSourceColumn.stdCol.expr.toString().replaceAll("path.override_c", "").contains("path"))
    assertResult(false)(parseOutputStructFieldWithMetadataSourceColumn.errors.expr.toString().contains("path.c"))
    assertResult(true)(parseOutputStructFieldWithMetadataSourceColumn.errors.expr.toString().contains("path.override_c"))
    assertResult(false)(parseOutputStructFieldWithMetadataSourceColumn.errors.expr.toString().replaceAll("path.override_c", "").contains("path"))
  }
}
