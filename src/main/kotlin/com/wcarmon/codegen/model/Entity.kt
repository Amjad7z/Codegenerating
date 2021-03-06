package com.wcarmon.codegen.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.wcarmon.codegen.DEBUG_MODE
import com.wcarmon.codegen.model.TargetLanguage.*
import com.wcarmon.codegen.view.*

/**
 * See src/main/resources/json-schema/entity.schema.json
 *
 * Represents ...
 * - REST: Resource
 * - Protocol buffers: Message
 * - RDBMS: Table
 *
 * - Kotlin: data class, POJO class
 * - Java: Record, POJO class
 * - Golang: struct
 * - Rust: struct
 * - c: struct
 * - c++: class (heap) or struct (stack)
 * - Typescript: interface or class
 */
@JsonIgnoreProperties("\u0024schema", "\u0024id")
@JsonPropertyOrder(alphabetic = true)
data class Entity(
  val name: Name,
  val pkg: PackageName,

  val canCheckForExistence: Boolean = true,
  val canCreate: Boolean = true,
  val canDelete: Boolean = true,
  val canExtend: Boolean = false,
  val canFindById: Boolean = true,
  val canList: Boolean = true,
  val canUpdate: Boolean = true,

  /**
   * Automatically updated when created
   */
  val createdTimestampFieldName: Name? = null,

  /** No leading comment markers (no leading slashes, no leading asterisk) */
  val documentation: List<String> = listOf(),

  // Likely easier to specify directly in template
  // unique, order matters
  val extraImplements: List<String> = listOf(),

  @JsonProperty("fields")
  private val ownFields: List<Field> = listOf(),

  @JsonProperty("fieldUris")
  @JsonDeserialize(using = FieldUriDeserializer::class)
  private val referencedFields: List<Field> = listOf(),

  /**
   * ID fields are implicitly indexed
   */
  val indexes: Set<Index> = setOf(),

  @JsonProperty("rdbms")
  val rdbmsConfig: RDBMSTableConfig = RDBMSTableConfig(),

  /**
   * Automatically updated when modified/updated
   */
  val updatedTimestampFieldName: Name? = null,

  val interFieldValidations: Set<InterFieldValidation> = setOf(),

  @JsonProperty("test")
  val testConfig: TestEntityConfig = TestEntityConfig(),

// TODO: list: pagination
) {

  /* All fields, in idiomatic order */
  val fields: List<Field> = ownFields + referencedFields

  init {
    require(fields.isNotEmpty()) { "At least one field required on Entity: name=$name" }

    // -- Validate field names are unique/distinct
    val fieldNames = fields.map { it.name }

    val duplicateFieldNames =
      fieldNames
        .groupBy { it }
        .filter { it.value.size > 1 }
        .map { it.key.lowerCamel }

    require(duplicateFieldNames.isEmpty()) {
      "field names must be unique: entity=${name.lowerCamel}, duplicates=${duplicateFieldNames}"
    }


    // -- Validate Id/PrimaryKey fields
    val idPositions = fields
      .filter { it.positionInId != null }
      .map { it.positionInId!! }
      .sortedBy { it }

    val duplicateIdPositions =
      idPositions
        .groupBy { it }
        .filter { it.value.size > 1 }
        .map { it.key }

    require(duplicateIdPositions.isEmpty()) {
      "All Id/PrimaryKey field positions must be unique: ${idPositions.sortedBy { it }}, duplicates=${duplicateIdPositions}"
    }

    require(idPositions.minOf { it } == 0) {
      "First index position must be 0, idPositions=${idPositions}"
    }

    val maxIndexPosition = idPositions.size - 1
    require(idPositions.maxOf { it } == maxIndexPosition) {
      "Last index position must be ${maxIndexPosition}, idPositions=${idPositions}"
    }

    // -- Validate extraImplements
    val duplicateExtraImplements =
      extraImplements
        .groupBy { it }
        .filter { it.value.size > 1 }
        .map { it.key }

    require(duplicateExtraImplements.isEmpty()) {
      "remove duplicate extraImplements values: $extraImplements, duplicates=${duplicateExtraImplements}"
    }

    // -- Validate indexes
    indexes
      .flatMap { it.fieldNames }
      .distinct()
      .forEach { indexFieldName ->
        require(fields.any { it.name == indexFieldName }) {
          "Cannot find property on ${name.upperCamel} matching the Index field: ${indexFieldName.lowerCamel}"
        }
      }

    val idFields = fields
      .filter { it.positionInId != null }

    indexes
      .filter { it.size == 1 }
      .forEach { index ->
        require(idFields.none { it.name == index.first }) {
          "Remove unnecessary index for ID field: ${name.upperCamel}.${index.first.lowerCamel}"
        }
      }

    if (createdTimestampFieldName != null) {
      val matches = (ownFields + referencedFields)
        .filter { field ->
          field.name == createdTimestampFieldName
        }

      require(matches.isNotEmpty()) {
        "Cannot find field matching createdTimestampFieldName=$createdTimestampFieldName"
      }

      //NOTE: duplicate field check above coveres the muti-match case
    }

    if (updatedTimestampFieldName != null) {
      val matches = (ownFields + referencedFields)
        .filter { field ->
          field.name == updatedTimestampFieldName
        }

      require(matches.isNotEmpty()) {
        "Cannot find field matching updateTimestampFieldName=$updatedTimestampFieldName"
      }

      //NOTE: duplicate field check above coveres the muti-match case
    }


    // -- InterField validation
    val allFields = (ownFields + referencedFields)
    interFieldValidations.forEach { v ->

      require(allFields.any { field ->
        field.name == v.fieldName0
      }) {
        "Cannot find field matching interFieldValidation.fieldName0: ${v.fieldName0}, entity=${this}"
      }

      require(allFields.any { field ->
        field.name == v.fieldName1
      }) {
        "Cannot find field matching interFieldValidation.fieldName1: ${v.fieldName1}, entity=${this}"
      }
    }
  }

  val java8View by lazy {
    Java8EntityView(
      debugMode = DEBUG_MODE,
      entity = this,
      jvmView = jvmView,
      rdbmsView = rdbmsView,
      targetLanguage = JAVA_08,
    )
  }

  val kotlinView by lazy {
    KotlinEntityView(
      debugMode = DEBUG_MODE,
      entity = this,
      jvmView = jvmView,
      rdbmsView = rdbmsView,
      targetLanguage = KOTLIN_JVM_1_4,
    )
  }

  val golangView by lazy {
    GolangEntityView(
      debugMode = DEBUG_MODE,
      entity = this,
      rdbmsView = rdbmsView,
      targetLanguage = GOLANG_1_9,
    )
  }

  val jvmView by lazy {
    JVMEntityView(
      debugMode = DEBUG_MODE,
      entity = this,
    )
  }

  val rdbmsView: RDBMSTableView by lazy {
    RDBMSTableView(
      debugMode = DEBUG_MODE,
      entity = this,
    )
  }

  val protobufView by lazy {
    ProtobufEntityView(
      debugMode = DEBUG_MODE,
      entity = this,
      targetLanguage = PROTO_BUF_3,
    )
  }

  val sqlView by lazy {
    RDBMSTableView(
      debugMode = DEBUG_MODE,
      entity = this,
    )
  }

  val sqlDelightView by lazy {
    SQLDelightTableView(
      debugMode = DEBUG_MODE,
      entity = this,
    )
  }

  val idFields = fields
    .filter { it.positionInId != null }
    .sortedBy { it.positionInId!! }

  val nonIdFields = fields
    .filter { it.positionInId == null }
    .sortedBy { it.name.lowerCamel }

  val patchableFields = nonIdFields
    .filter { it.canUpdate }

  val hasIdFields: Boolean = idFields.isNotEmpty()

  val hasNonIdFields: Boolean = nonIdFields.isNotEmpty()

  val sortedFields = fields.sortedBy { it.name.lowerCamel }

  val sortedFieldsWithIdsFirst =
    idFields + nonIdFields

  val updatedTimestampField: Field? by lazy {
    fields
      .filter { field ->
        field.name == updatedTimestampFieldName
      }
      .also {
        check(it.size <= 1) {
          "Cannot have multiple updateTimestamp fields"
        }
      }
      .firstOrNull()
  }

  fun fieldForName(candidate: Name): Field? =
    fields.firstOrNull { field ->
      field.name == candidate
    }
}
