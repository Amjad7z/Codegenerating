package com.wcarmon.codegen.view

import com.wcarmon.codegen.ast.*
import com.wcarmon.codegen.ast.FieldReadMode.DIRECT
import com.wcarmon.codegen.model.BaseFieldType.LIST
import com.wcarmon.codegen.model.BaseFieldType.SET
import com.wcarmon.codegen.model.Entity
import com.wcarmon.codegen.model.JDBCColumnIndex.Companion.FIRST
import com.wcarmon.codegen.model.PreparedStatementBuilderConfig
import com.wcarmon.codegen.model.SerdeMode
import com.wcarmon.codegen.model.TargetLanguage
import com.wcarmon.codegen.util.buildPreparedStatementSetters
import com.wcarmon.codegen.util.effectiveProtoSerde
import com.wcarmon.codegen.util.getKotlinImportsForFields
import com.wcarmon.codegen.util.kotlinMethodArgsForFields

/**
 * Kotlin related convenience methods for a [Entity]
 */
class KotlinEntityView(
  private val debugMode: Boolean,
  private val entity: Entity,
  private val jvmView: JVMEntityView,
  private val rdbmsView: RDBMSTableView,
  private val targetLanguage: TargetLanguage,
) {

  init {
    require(targetLanguage.isKotlin) {
      "invalid target language: $targetLanguage"
    }
  }

  private val renderConfig = RenderConfig(
    debugMode = debugMode,
    targetLanguage = targetLanguage,
    terminate = false
  )

  //TODO: supporting suffix is hard from freemarker
  fun documentation(
    vararg prefix: String,
  ) = DocumentationExpression(
    parts = prefix.toList() + entity.documentation,
  )
    .render(config = renderConfig)

  val importsForFields: Set<String> = getKotlinImportsForFields(entity)

  val insertPreparedStatementSetterStatements by lazy {
    rdbmsView.insertPreparedStatementSetterStatements(renderConfig)
  }

  val preparedStatementSetterStatementsForPK by lazy {

    val psConfig = PreparedStatementBuilderConfig(
      allowFieldNonNullAssertion = false,
      fieldOwner = EmptyExpression,
      fieldReadMode = DIRECT,
      preparedStatementIdentifierExpression = RawLiteralExpression("ps"),
    )

    buildPreparedStatementSetters(
      fields = entity.idFields,
      firstIndex = FIRST,
      psConfig = psConfig,
    )
      .joinToString(separator = "\n") {
        it.render(renderConfig)
      }
  }

  val updatePreparedStatementSetterStatements by lazy {
    rdbmsView.updatePreparedStatementSetterStatements(renderConfig)
  }

  val fieldDeclarations: String by lazy {

    entity.sortedFieldsWithIdsFirst
      .joinToString("\n") { field ->

        val defaultValueExpression = DefaultValueExpression(
          defaultValue = field.defaultValue,
        )

        FieldDeclarationExpression(
          //TODO: suffix "ID/Primary key" when `field.idField`
          defaultValue = defaultValueExpression,
          documentation = DocumentationExpression(field.documentation),
          field = field,
          finalityModifier = FinalityModifier.FINAL,
          visibilityModifier = VisibilityModifier.PRIVATE,
        )
          .render(
            renderConfig.copy(lineIndentation = "  "))
      }
  }

  val typeReferenceDeclarations: String by lazy {
    val indentation = "  "

    jvmView.collectionFields
      .joinToString("\n") { field ->
        val output = StringBuilder(512)

        val collectionLiteral = when (field.jvmConfig.effectiveBaseType) {
          SET -> "Set"
          LIST -> "List"
          else -> TODO("Add typeref support for field=$field, entity=$entity")
        }

        //TODO: should I use field.jvmView.jacksonTypeRef
        output.append("val ${entity.name.upperSnake}__${field.name.upperSnake}_TYPE_REF")
        output.append(": TypeReference<$collectionLiteral<${field.jvmConfig.typeParameters.first()}>> ")
        output.append("=\n")
        output.append(indentation)
        output.append("object : TypeReference<$collectionLiteral<${field.jvmConfig.typeParameters.first()}>>() {}")

        output.toString()
      }
  }

  /**
   * setter methods on a Proto builder
   */
  val entityToProtoSetters: String by lazy {

    entity.sortedFieldsWithIdsFirst
      .joinToString(
        separator = "\n"
      ) { field ->

        val read = FieldReadExpression(
          assertNonNull = false,
          fieldName = field.name,
          fieldOwner = RawLiteralExpression("entity"),
          overrideFieldReadMode = DIRECT,
        )

        val wrapMe =
          if (!field.type.nullable || field.defaultValue.isAbsent) {
            read

          } else {
            DefaultWhenNullExpression(
              primaryExpression = read,
              defaultValueExpression = DefaultValueExpression(
                defaultValue = field.defaultValue,
              )
            )
          }

        val wrapper = WrapWithSerdeExpression(
          serde = effectiveProtoSerde(field),
          serdeMode = SerdeMode.SERIALIZE,
          wrapped = wrapMe,
        )

        ProtoFieldWriteExpression(
          field = field,
          sourceReadExpression = wrapper,
        )
          .render(renderConfig.unterminated)
      }
  }

  val protoToEntitySetters: String by lazy {
    entity.sortedFieldsWithIdsFirst
      .joinToString(
        separator = "\n",
      ) { field ->

        val read = ProtoFieldReadExpression(
          assertNonNull = false,
          field = field,
          fieldOwner = RawLiteralExpression("proto"),
          serde = effectiveProtoSerde(field),
        )

        val renderedRead = read.render(renderConfig.unterminated)
        "${field.name.lowerCamel} = $renderedRead,"
      }
  }

  val validatedFields =
    entity.sortedFieldsWithIdsFirst
      .filter {
        it.validationConfig.hasValidation
      }

  val validationExpressions: String by lazy {

    validatedFields.map { field ->
      FieldValidationExpressions(
        field = field,
        validationConfig = field.validationConfig,
        validationSeparator = "\n"
      )
        .render(renderConfig.doubleIndented)
    }
      .filter { it.isNotBlank() }
      .joinToString(
        separator = "\n\n",
      )
  }

  fun patchQueries(): String {
    val indentation = "  "

    if (!entity.hasIdFields || !entity.hasNonIdFields) {
      // need ID fields for WHERE clause
      return ""
    }

    if (!entity.canUpdate) {
      return ""
    }

    return entity.nonIdFields
      .map { field ->
        val lines = mutableListOf<String>()

        lines += "const val PATCH__${entity.name.upperSnake}__${field.name.upperSnake} ="
        lines += """$indentation"UPDATE \"${entity.name.lowerSnake}\" " +"""
        lines += """$indentation"SET ${field.name.lowerSnake}=? " + """

        if (entity.updatedTimestampFieldName != null && !field.jvmView.isUpdatedTimestamp) {
          lines += """$indentation"AND ${entity.updatedTimestampFieldName.lowerSnake}=? " + """
        }

        lines += """$indentation"WHERE ${entity.rdbmsView.primaryKeyWhereClause}";"""

        lines.joinToString(
          separator = "\n"
        ) {
          "$indentation$it"
        }

      }.joinToString(
        separator = "\n\n"
      ) {
        "$indentation$it"
      }
  }

  fun methodArgsForIdFields(qualified: Boolean) =
    kotlinMethodArgsForFields(entity.idFields, qualified)
}
