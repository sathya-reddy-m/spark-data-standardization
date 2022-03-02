package za.co.absa.standardization

/*
 * Copyright 2018 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, Row, SparkSession}

object DataFrameTestUtils {

  implicit class RowSeqToDf(val data: Seq[Row]) extends AnyVal {
    def toDfWithSchema(schema: StructType)(implicit spark: SparkSession): DataFrame = {
      spark.createDataFrame(spark.sparkContext.parallelize(data), schema)
    }
  }
}
