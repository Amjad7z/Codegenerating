package com.wcarmon.codegen.view

import com.wcarmon.codegen.ast.*
import com.wcarmon.codegen.ast.FieldReadMode.DIRECT
import com.wcarmon.codegen.ast.FieldReadMode.GETTER
import com.wcarmon.codegen.model.BaseFieldType
import com.wcarmon.codegen.model.Entity
import com.wcarmon.codegen.model.JDBCColumnIndex.Companion.FIRST
import com.wcarmon.codegen.model.PreparedStatementBuilderConfig
import com.wcarmon.codegen.model.SerdeMode.SERIALIZE
import com.wcarmon.codegen.model.TargetLanguage
import com.wcarmon.codegen.model.TargetLanguage.JAVA_08
import com.wcarmon.codegen.util.buildJavaPreconditionStatements
import com.wcarmon.codegen.util.buildPreparedStatementSetters
import com.wcarmon.codegen.util.javaImportsForFields

/**
 * Java related convenience methods for a [Entity]
 */
class Java8EntityView(
  debugMode: Boolean,
  private val entity: Entity,
  private val jvmView: JVMEntityView,
  private val rdbmsView: RDBMSTableView,
  targetLanguage: TargetLanguage = JAVA_08,
) {

  init {
    require(targetLanguage.isJava) {
      "invalid target language: $targetLanguage"
    }
  }

  private val renderConfig = RenderConfig(
    debugMode = debugMode,
    lineIndentation = "",
    targetLanguage = targetLanguage,
    terminate = true,
  )

  fun documentation(
    vararg prefix: String,
  ) = DocumentationExpression(
    parts = prefix.toList() + entity.documentation,
  )
    .render(config = renderConfig)

  val importsForFields: Set<String> = javaImportsForFields(entity)

  val insertPreparedStatementSetterStatements by lazy {
    rdbmsView.insertPreparedStatementSetterStatements(renderConfig)
  }

  val primaryKeyPreconditionStatements =
    buildJavaPreconditionStatements(entity.idFields)
      .joinToString("\n\t")

  val preparedStatementSetterStatementsForPK by lazy {

    val psConfig = PreparedStatementBuilderConfig(
      allowFieldNonNullAssertion = false,
      fieldOwner = EmptyExpression,
      fieldReadMode = DIRECT,
      preparedStatementIdentifierExpression = RawLiteralExpression("ps"),
    )

    //TODO: this needs termination

    buildPreparedStatementSetters(
      fields = entity.idFields,
      firstIndex = FIRST,
      psConfig = psConfig,
    )
      .joinToString(separator = "\n") {
        it.render(renderConfig.terminated)
      }
  }

  val updatePreparedStatementSetterStatements by lazy {
    rdbmsView.updatePreparedStatementSetterStatements(renderConfig)
  }

  val fieldDeclarations: String by lazy {

    entity.sortedFieldsWithIdsFirst
      .joinToString("\n") { field ->
        FieldDeclarationExpression(
          //TODO: suffix "ID/Primary key" when `field.idField`
          documentation = DocumentationExpression(field.documentation),
          field = field,
          finalityModifier = FinalityModifier.FINAL,
          visibilityModifier = VisibilityModifier.PRIVATE,
//      defaultValue = TODO()  TODO: fix this
        ).render(renderConfig.indented)
      }
  }


  val protoToEntitySetters: String by lazy {
    entity.sortedFieldsWithIdsFirst
      .joinToString(
        separator = "\n",
      ) { field ->

        val read = ProtobufFieldReadExpression(
          assertNonNull = false,
          field = field,
          fieldOwner = RawLiteralExpression("proto"),
          serde = field.effectiveProtobufSerde(JAVA_08),
        )

        val renderedRead = read.render(renderConfig.unterminated)
        ".${field.name.lowerCamel}($renderedRead)"
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
          overrideFieldReadMode = GETTER,
        )

        val wrapper = WrapWithSerdeExpression(
          serde = field.effectiveProtobufSerde(JAVA_08),
          serdeMode = SERIALIZE,
          wrapped = read,
        )

        ProtobufFieldWriteExpression(
          field = field,
          sourceReadExpression = wrapper,
        )
          .render(renderConfig.unterminated)
      }
  }


  val typeReferenceDeclarations: String by lazy {
    val indentation = "  "

    jvmView.collectionFields
      .joinToString("\n") { field ->
        val output = StringBuilder(512)

        val collectionLiteral = when (field.effectiveBaseType(JAVA_08)) {
          BaseFieldType.SET -> "Set"
          BaseFieldType.LIST -> "List"
          else -> TODO("Add typeref support for field=$field, entity=$entity")
        }

        val typeParameters = field.typeParameters(JAVA_08)

        //TODO: should I use field.jvmView.jacksonTypeRef
        output.append("public static final TypeReference<$collectionLiteral<${typeParameters.first()}>> ")
        output.append("${entity.name.upperSnake}__${field.name.upperSnake}_TYPE_REF ")
        output.append("=\n")
        output.append(indentation)
        output.append("new TypeReference<>(){};")

        output.toString()
      }
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

  //TODO: indentation
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

        lines += "public static final String PATCH__${entity.name.upperSnake}__${field.name.upperSnake} ="
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

  private val validatedFields =
    entity.sortedFieldsWithIdsFirst
      .filter {
        it.validationConfig.hasValidation
            // For Java we need validation to enforce null safety
            || !it.type.nullable
      }

  //For freemarker
  fun methodArgsForIdFields(qualified: Boolean) =
    methodArgsForIdFields(qualified, "")

  fun methodArgsForIdFields(
    qualified: Boolean,
    annotationForArgument: String,
  ): String {

    require(annotationForArgument.trim() == annotationForArgument) {
      "annotation must be trimmed: $annotationForArgument"
    }

    val annotations =
      if (annotationForArgument.isBlank()) {
        listOf()
      } else {
        val annotationExpression = AnnotationExpression(
          name = annotationForArgument,
        )

        // Use same annotation for all
        (1..entity.idFields.size)
          .map { annotationExpression }
      }

    return entity.idFields.joinToString(
      separator = ", "
    ) { field ->
      MethodParameterExpression(
        annotations = annotations,
        field = field,
        finalityModifier = FinalityModifier.FINAL,
        qualified = qualified,
      )
        .render(renderConfig)
    }
  }

}
