package com.wcarmon.codegen.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder

/**
 * [Field] attributes specific to a field for golang
 *
 * See src/main/resources/json-schema/golang-field.schema.json
 */
@JsonPropertyOrder(alphabetic = true)
data class GolangFieldConfig(

  val defaultValue: DefaultValue = DefaultValue(),

  /**
   * Replace the auto-derived type with this literal
   */
  val overrideEffectiveType: String = "",

  val overrideElasticSearchSerde: Serde? = null,
  val overrideKafkaSerde: Serde? = null,
  val overrideProtobufRepeatedItemSerde: Serde? = null,
  val overrideProtobufSerde: Serde? = null,
  val overrideRDBMSSerde: Serde? = null,

  @JsonProperty("validation")
  val validationConfig: FieldValidation = FieldValidation(),

  // -- Only for Collections & Generic types (Parametric polymorphism)
  val typeParameters: List<String> = listOf(),

  @JsonProperty("test")
  val testConfig: TestFieldConfig = TestFieldConfig(),
) {

  val overrideBaseType: BaseFieldType? =
    if (overrideEffectiveType.isNotBlank()) {
      BaseFieldType.parse(overrideEffectiveType)
    } else {
      null
    }
}
