package com.wcarmon.codegen.model

import com.fasterxml.jackson.annotation.JsonPropertyOrder

/**
 * [Entity] attributes specific to relational database Table
 *
 * See src/main/resources/json-schema/rdbms-table.schema.json
 */
@JsonPropertyOrder(alphabetic = true)
data class RDBMSTableConfig(
  val schema: String = "",

  // TODO: list: order by fieldX, asc|desc
) {

  init {
    require(schema == schema.trim()) {
      "schema must be trimmed: $schema"
    }

    //TODO: enforce any sensible schema restrictions (eg. charset)
  }
}
