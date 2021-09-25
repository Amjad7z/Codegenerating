package com.wcarmon.codegen.model

import com.fasterxml.jackson.annotation.JsonCreator


/**
 * In JSON config...
 *
 * No default (Absent):
 *   1. (omit Field.defaultValue)
 *   2. ..., "default": null, ...
 *   3. ..., "default": {}, ...
 *
 * Default to null:
 *   1. ..., "defaultValue": {"value": null}, ...
 *
 * Non-null default value:
 *   1. ..., "default": {"value": "foo"}, ...
 *   2. ..., "default": {"value": 3.14}, ...
 *   3. ..., "default": {"value": 7}, ...
 *   4. ..., "default": {"value": false}, ...
 *   5. ..., "default": {"value": true}, ...
 *   6. ..., "default": {"value": ""}, ...
 *
 *  See tests for more examples [DefaultValueTest]
 */
data class DefaultValue(
  private val wrapper: ValueWrapper? = null,

  private val presentAndNull: Boolean = false,
) {

  // Must wrap the value to distinguish absence from null
  data class ValueWrapper(
    val value: Any?,
  )

  companion object {

    val NO_DEFAULT = DefaultValue(null)
    private const val PROEPRTY_NAME_FOR_WRAPPER = "defaultValue"
    private const val PROPERTY_NAME_FOR_VALUE = "value"

    @JsonCreator
    @JvmStatic
    fun build(raw: Any?): DefaultValue {
      if (raw == null) {
        return NO_DEFAULT
      }

      check(raw is Map<*, *>) {
        "Invalid defaultValue, must be an object or null.  " +
            "See DefaultValue documentation: value=$raw"
      }

      val jsonObj: Map<*, *> = raw
      if (jsonObj.isEmpty()) {
        return NO_DEFAULT
      }

//      val hasValue = jsonObj.containsKey(PROEPRTY_NAME_FOR_WRAPPER)
      val realDefault = jsonObj[PROEPRTY_NAME_FOR_WRAPPER] ?: return NO_DEFAULT

      return DefaultValue(
        ValueWrapper(realDefault))

//      check(wrapperObj is Map<*, *>) {
//        "Invalid JSON for default value (must be an object): json=$jsonObj"
//      }
//
//      if (wrapperObj.isEmpty()) return NO_DEFAULT
//      return DefaultValue(ValueWrapper(wrapperObj[PROPERTY_NAME_FOR_VALUE]))
    }
  }

  /** isPresent, isNotAbsent, wasSetByUser, isProvided, hasDefault, hasValue */
  val isPresent: Boolean = wrapper != null

  val isAbsent: Boolean = wrapper == null

  /**
   * Only readable when [isPresent]
   * User wants an explicit default to null/nil/NULL
   */
  val isNullLiteral: Boolean by lazy {
    check(isPresent) {
      "only read when hasValue==true"
    }

    wrapper != null && wrapper.value == null
  }

  val isEmptyString: Boolean by lazy {
    check(isPresent) {
      "only read when hasValue==true"
    }

    wrapper?.value != null &&
        wrapper.value is String &&
        wrapper.value.isEmpty()
  }

  val isBlankString: Boolean by lazy {
    check(isPresent) {
      "only read when hasValue==true"
    }

    wrapper?.value != null &&
        wrapper.value is String &&
        wrapper.value.isBlank()
  }

  /** Only readable when [isPresent] */
  val literal: Any? by lazy {
    check(wrapper != null) {
      "defaultValue only readable when present (check with field.defaultValue.isPresent)"
    }

    wrapper.value
  }
}
