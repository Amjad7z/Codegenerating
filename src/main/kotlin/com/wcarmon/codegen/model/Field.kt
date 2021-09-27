package com.wcarmon.codegen.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.wcarmon.codegen.DEBUG_MODE
import com.wcarmon.codegen.model.BaseFieldType.*
import com.wcarmon.codegen.model.TargetLanguage.*
import com.wcarmon.codegen.util.*
import com.wcarmon.codegen.view.*
import org.apache.logging.log4j.LogManager

/**
 * See src/main/resources/json-schema/field.schema.json
 *
 * Represents ...
 * - REST: resource property/attribute
 * - Protocol buffers: field
 * - RDBMS: column
 *
 * - C++: struct member, class data member
 * - C: struct member
 * - Golang: struct field
 * - Java: class field, record field
 * - Kotlin: class field
 * - Rust: struct field
 * - Typescript: property
 *
 * See extensions package for laguage & framework specific methods
 */
// `$id` and `$schema` are part of json standard, but not useful for code generation
@JsonIgnoreProperties("\u0024schema", "\u0024id")
@JsonPropertyOrder(alphabetic = true)
data class Field(
  val name: Name,

  /**
   * Might be (logically) overriden by tech specific config below
   */
  val type: LogicalFieldType,

  /**
   * always false for id fields
   */
  val canUpdate: Boolean = true,

  val canLog: Boolean = true,

  val defaultValue: DefaultValue = DefaultValue(),

  /** No leading comment markers (no leading slashes, no leading asterisk) */
  val documentation: List<String> = listOf(),

  /**
   * null:  not a Id/PK field
   * 0:     1st part of Id/PK field
   * 1:     2nd part of Id/PK field
   * ...
   */
  val positionInId: Int? = null,
  val validationConfig: FieldValidation = FieldValidation(),

  // -- Technology specific config
  private val golangConfig: GolangFieldConfig = GolangFieldConfig(),
  private val jvmConfig: JVMFieldConfig = JVMFieldConfig(),
  private val protobufConfig: ProtobufFieldConfig = ProtobufFieldConfig(),

  //TODO: mark private
  val rdbmsConfig: RDBMSColumnConfig = RDBMSColumnConfig(),
) {

  companion object {

    @JvmStatic
    private val LOG = LogManager.getLogger(Field::class.java)

    /**
     * Builder to handle the impedance mismatch between json and in-memory structures
     *
     * GOTCHA: have to manually keep this in sync with src/main/resources/json-schema/field.schema.json
     */
    @JvmStatic
    @JsonCreator
    fun parse(
      @JsonProperty("canLog") canLog: Boolean?,
      @JsonProperty("canUpdate") canUpdate: Boolean?,
      @JsonProperty("defaultValue") defaultValue: DefaultValue?,
      @JsonProperty("documentation") documentation: Iterable<String>?,
      @JsonProperty("enumType") enumType: Boolean?,
      @JsonProperty("golang") golangConfig: GolangFieldConfig?,
      @JsonProperty("jvm") jvmFieldConfig: JVMFieldConfig?,
      @JsonProperty("name") name: Name,
      @JsonProperty("nullable") nullable: Boolean?,
      @JsonProperty("positionInId") positionInId: Int?,
      @JsonProperty("precision") precision: Int?,
      @JsonProperty("protobuf") protobufConfig: ProtobufFieldConfig?,
      @JsonProperty("rdbms") rdbmsConfig: RDBMSColumnConfig?,
      @JsonProperty("scale") scale: Int?,
      @JsonProperty("signed") signed: Boolean?,
      @JsonProperty("type") typeLiteral: String?,
      @JsonProperty("validation") validationConfig: FieldValidation?,
    ): Field {

      require(typeLiteral?.isNotBlank() ?: false) {
        "Field.type is required: this=$this, name=$name"
      }

      //TODO: signed should override whatever is specified on type literal

      val logicalType = LogicalFieldType(
        base = BaseFieldType.parse(typeLiteral ?: ""),
        enumType = enumType ?: false,
        nullable = nullable ?: false,
        precision = precision,
        rawTypeLiteral = typeLiteral ?: "",
        scale = scale ?: 0,
        signed = signed ?: true,
      )

      return Field(
        canLog = canLog ?: true,
        canUpdate = canUpdate ?: true,
        defaultValue = defaultValue ?: DefaultValue(),
        documentation = documentation?.toList() ?: listOf(),
        golangConfig = golangConfig ?: GolangFieldConfig(),
        jvmConfig = jvmFieldConfig ?: JVMFieldConfig(),
        name = name,
        positionInId = positionInId,
        protobufConfig = protobufConfig ?: ProtobufFieldConfig(),
        rdbmsConfig = rdbmsConfig ?: RDBMSColumnConfig(),
        type = logicalType,
        validationConfig = validationConfig ?: FieldValidation(),
      )
    }
  }

  init {

    val isIdField = (positionInId ?: -1) >= 0
    if (isIdField) {
      require(!type.nullable) {
        "Id/PrimaryKey fields cannot be nullable: $this"
      }
    }

    //NOTE: precision and scale are validated on LogicalFieldType

    if (positionInId != null) {
      require(positionInId >= 0) {
        "positionInId must be non-negative: $positionInId, this=$this"
      }
    }

    assertTypeParametersValid(GOLANG_1_9)
    assertTypeParametersValid(JAVA_08)
    assertTypeParametersValid(KOTLIN_JVM_1_4)
    assertTypeParametersValid(PROTO_BUF_3)
  }

  val java8View by lazy {
    Java8FieldView(
      debugMode = DEBUG_MODE,
      field = this,
      jvmView = jvmView,
      rdbmsView = rdbmsView,
      targetLanguage = JAVA_08,
    )
  }

  val kotlinView by lazy {
    KotlinFieldView(
      debugMode = DEBUG_MODE,
      field = this,
      jvmView = jvmView,
      rdbmsView = rdbmsView,
      targetLanguage = KOTLIN_JVM_1_4,
    )
  }

  val golangView by lazy {
    GolangFieldView(
      debugMode = DEBUG_MODE,
      field = this,
      rdbmsView = rdbmsView,
      targetLanguage = GOLANG_1_9,
    )
  }


  val rdbmsView by lazy {
    RDBMSColumnView(
      debugMode = DEBUG_MODE,
      field = this,
    )
  }

  val protobufView by lazy {
    ProtobufFieldView(
      debugMode = DEBUG_MODE,
      field = this,
      targetLanguage = PROTO_BUF_3,
    )
  }

  val sqlView by lazy {
    RDBMSColumnView(
      debugMode = DEBUG_MODE,
      field = this,
    )
  }

  val sqlDelightView by lazy {
    SQLDelightColumnView(
      debugMode = DEBUG_MODE,
      field = this,
    )
  }

  val isIdField: Boolean = (positionInId ?: -1) >= 0

  val jvmView by lazy {
    JVMFieldView(
      field = this,
      debugMode = DEBUG_MODE)
  }

  fun effectiveBaseType(targetLanguage: TargetLanguage): BaseFieldType =
    when (targetLanguage) {
      GOLANG_1_9 -> golangConfig.overrideBaseType ?: type.base

      JAVA_08,
      JAVA_11,
      JAVA_17,
      -> type.base

      KOTLIN_JVM_1_4 -> type.base

      PROTO_BUF_3 -> protobufConfig.overrideBaseType ?: type.base

      SQL_DB2,
      SQL_H2,
      SQL_MARIA,
      SQL_MYSQL,
      SQL_ORACLE,
      SQL_POSTGRESQL,
      SQL_SQLITE,
      -> rdbmsConfig.overrideBaseType ?: type.base

      SQL_DELIGHT -> rdbmsConfig.overrideBaseType ?: type.base

      else -> TODO("Get effective base type for field=$this, targetLanguage=$targetLanguage")
    }

  fun typeParameters(targetLanguage: TargetLanguage): List<String> =
    when (targetLanguage) {
      GOLANG_1_9,
      -> golangConfig.typeParameters

      JAVA_08,
      JAVA_11,
      JAVA_17,
      KOTLIN_JVM_1_4,
      -> jvmConfig.typeParameters

      PROTO_BUF_3,
      -> listOf() //TODO: fix this when you support proto collections

      //TODO: more here
      else -> TODO("get type params for field=$this, targetLanguage=$targetLanguage")
    }

  /** true for String, Collections, Enums, Arrays */
  fun isParameterized(targetLanguage: TargetLanguage) =
    when (effectiveBaseType(targetLanguage)) {
      ARRAY,
      LIST,
      MAP,
      SET,
      -> true

      USER_DEFINED -> typeParameters(targetLanguage).isNotEmpty()

      else -> false
    }


  fun overrideDefaultValue(targetLanguage: TargetLanguage): DefaultValue =
    //TODO: improve this
    when (targetLanguage) {
      JAVA_08,
      JAVA_11,
      JAVA_17,
      KOTLIN_JVM_1_4,
      -> defaultValue

      GOLANG_1_9,
      -> defaultValue

      SQL_DELIGHT,
      -> defaultValue

      //TODO: more here
      else -> TODO("get overrideDefaultValue: targetLanguage=$targetLanguage, field=$this")
    }


  fun effectiveTypeLiteral(
    targetLanguage: TargetLanguage,
    fullyQualified: Boolean = true,
  ): String = when (targetLanguage) {

    JAVA_08,
    JAVA_11,
    JAVA_17,
    -> javaTypeLiteral(this, fullyQualified)

    KOTLIN_JVM_1_4,
    -> kotlinTypeLiteral(this, fullyQualified)

    GOLANG_1_9,
    -> golangConfig.overrideTypeLiteral ?: golangTypeLiteral(this, fullyQualified)

    PROTO_BUF_3 -> protobufConfig.typeLiteral(type)

    SQL_POSTGRESQL -> rdbmsConfig.overrideTypeLiteral ?: getPostgresTypeLiteral(
      effectiveBaseType = effectiveBaseType(SQL_POSTGRESQL),
      errorLoggingInfo = "field=$this",
      logicalFieldType = type,
      rdbmsConfig = rdbmsConfig,
    )

    else -> TODO("get typeLiteral: targetLanguage=$targetLanguage, field=$this")
  }

  fun effectiveRDBMSSerde(targetLanguage: TargetLanguage): Serde =
    when (targetLanguage) {
      JAVA_08,
      JAVA_11,
      JAVA_17,
      KOTLIN_JVM_1_4,
      -> if (jvmConfig.overrideRDBMSSerde != null) {
        // -- User override is highest priority
        jvmConfig.overrideRDBMSSerde

      } else if (requiresJDBCSerde(this)) {
        //TODO: use default serde for INSTANT
        //TODO: use default serde for URI
        LOG.warn("We recommend you override the rdbms Serde on $this")
        Serde.INLINE

      } else {
        Serde.INLINE
      }

      GOLANG_1_9,
      -> if (golangConfig.overrideRDBMSSerde != null) {
        // -- User override is highest priority
        golangConfig.overrideRDBMSSerde

//      } else if (requiresGolangSQLSerde(this)) {
//        LOG.warn("We recommend you override the rdbms Serde on $this")
//        Serde.INLINE

      } else {
        Serde.INLINE
      }

      SQL_DB2,
      SQL_DELIGHT,
      SQL_H2,
      SQL_MARIA,
      SQL_MYSQL,
      SQL_ORACLE,
      SQL_POSTGRESQL,
      SQL_SQLITE,
      -> throw UnsupportedOperationException()

      else -> TODO("get effectiveSerde: targetLanguage=$targetLanguage, field=$this")
    }

  fun effectiveProtobufSerde(targetLanguage: TargetLanguage): Serde =
    when (targetLanguage) {
      JAVA_08,
      JAVA_11,
      JAVA_17,
      KOTLIN_JVM_1_4,
      ->
        if (jvmConfig.overrideProtobufSerde != null) {
          // -- User override is highest priority
          jvmConfig.overrideProtobufSerde

        } else if (effectiveBaseType(targetLanguage).isCollection) {
          defaultSerdeForCollection(this, targetLanguage)

        } else if (requiresProtobufSerde(this)) {
          LOG.warn("We recommend you override the protobuf Serde on $this")
          Serde.INLINE

        } else {
          Serde.INLINE
        }

      GOLANG_1_9 ->
        if (golangConfig.overrideProtobufSerde != null) {
          // -- User override is highest priority
          golangConfig.overrideProtobufSerde

        } else if (effectiveBaseType(targetLanguage).isCollection) {
          defaultSerdeForCollection(this, targetLanguage)

        } else if (requiresProtobufSerde(this)) {
          LOG.warn("We recommend you override the protobuf Serde on $this")
          Serde.INLINE

        } else {
          Serde.INLINE
        }

      // PROTO cannot be mapped to itself
      PROTO_BUF_3 -> throw UnsupportedOperationException("Programming error ")
      else -> TODO("get effectiveSerde: targetLanguage=$targetLanguage, field=$this")
    }

  fun effectiveProtobufSerdesForTypeParameters(targetLanguage: TargetLanguage): List<Serde> {
    return typeParameters(targetLanguage)
      .map {
        //TODO: improve this
        //        protobufConfig.overrideRepeatedItemSerde ?: Serde.INLINE
        Serde.INLINE
      }
  }

  /**
   * Parametric polymorphism
   */
  private fun assertTypeParametersValid(targetLanguage: TargetLanguage) {

    val typeParameters = typeParameters(targetLanguage)

    when (val n = effectiveBaseType(targetLanguage).requiredTypeParameterCount) {
      0 -> require(typeParameters.isEmpty()) {
        "type parameter not allowed: field=$this"
      }

      1 -> require(typeParameters.size == n) {
        "exactly 1-type parameter required (add 'typeParameters' to Field): field=$this"
      }

      else -> require(typeParameters.size == n) {
        "type parameters required (add 'typeParameters' to Field): " +
            "requiredCount=$n, actualCount=${typeParameters.size}, this=$this"
      }
    }
  }

}
