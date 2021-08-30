package com.wcarmon.codegen.view

import com.wcarmon.codegen.ast.RawExpression
import com.wcarmon.codegen.model.Entity
import com.wcarmon.codegen.model.JDBCColumnIndex
import com.wcarmon.codegen.model.PreparedStatementBuilderConfig
import com.wcarmon.codegen.model.TargetLanguage
import com.wcarmon.codegen.model.TargetLanguage.JAVA_08
import com.wcarmon.codegen.model.TargetLanguage.KOTLIN_JVM_1_4
import com.wcarmon.codegen.util.*
import org.atteo.evo.inflector.English

/**
 * RDBMS related convenience methods for a [Entity]
 * See [com.wcarmon.codegen.model.RDBMSTableConfig]
 */
data class RDBMSTableView(
  private val entity: Entity,
) {

  val commaSeparatedColumns: String = commaSeparatedColumns(entity)

  //TODO: return Documentation
  val commentForPrimaryKeyFields: String =
    if (entity.idFields.isEmpty()) ""
    else "PrimaryKey " + English.plural("field", entity.idFields.size)

  val schemaPrefix: String =
    if (entity.rdbmsConfig.schema.isBlank() != false) ""
    else "${entity.rdbmsConfig.schema}."


  val primaryKeyWhereClause: String = commaSeparatedColumnAssignment(entity.idFields)

  val primaryKeyTableConstraint: String = primaryKeyTableConstraint(entity)

  val questionMarkStringForInsert: String = (1..entity.fields.size).joinToString { "?" }

  val updateSetClause: String = commaSeparatedColumnAssignment(entity.nonIdFields)

  val commaSeparatedPrimaryKeyIdentifiers: String by lazy {
    entity.idFields.joinToString(", ") { it.name.lowerCamel }
  }

  val jdbcSerializedPrimaryKeyFields by lazy {
    commaSeparatedJavaFields(entity.idFields)
  }

  // For INSERT, PrimaryKey fields are first
  fun insertPreparedStatementSetterStatements(
    targetLanguage: TargetLanguage,
  ): String {

    val cfg = PreparedStatementBuilderConfig(
      allowFieldNonNullAssertion = true, //TODO fix this
      fieldOwner = RawExpression("entity"),
      fieldReadMode = targetLanguage.fieldReadMode,
      preparedStatementIdentifierExpression = RawExpression("ps")
    )

    val pk = buildPreparedStatementSetters(
      cfg = cfg,
      fields = entity.idFields,
      firstIndex = JDBCColumnIndex.FIRST,
    )

    val nonPk = buildPreparedStatementSetters(
      cfg = cfg,
      fields = entity.nonIdFields,
      firstIndex = JDBCColumnIndex(entity.idFields.size + 1),
    )

    return (pk + nonPk)
      .map { it.render(targetLanguage) }
      .joinToString(separator = "\n")
  }

  // NOTE: For UPDATE, PrimaryKey fields are last
  fun updatePreparedStatementSetterStatements(
    targetLanguage: TargetLanguage,
  ): String {

    val cfg = PreparedStatementBuilderConfig(
      allowFieldNonNullAssertion = true, //TODO fix this
      fieldOwner = RawExpression("entity"),
      fieldReadMode = targetLanguage.fieldReadMode,
      preparedStatementIdentifierExpression = RawExpression("ps")
    )

    val nonPk = buildPreparedStatementSetters(
      cfg = cfg,
      fields = entity.nonIdFields,
      firstIndex = JDBCColumnIndex.FIRST,
    )

    val pk = buildPreparedStatementSetters(
      cfg = cfg,
      fields = entity.idFields,
      firstIndex = JDBCColumnIndex(entity.nonIdFields.size + 1),
    )

    val separator = RawExpression("\n\t\t// Primary key field(s)")

    return (nonPk + separator + pk)
      .map { it.render(targetLanguage) }
      .joinToString(separator = "\n")
  }

  fun preparedStatementSetterStatementsForPrimaryKey(
    config: PreparedStatementBuilderConfig = PreparedStatementBuilderConfig(),
    targetLanguage: TargetLanguage,
  ): String =
    buildPreparedStatementSetters(
      cfg = config,
      fields = entity.idFields,
      firstIndex = JDBCColumnIndex.FIRST,
    )
      .joinToString(separator = "\n") {
        it.render(targetLanguage)
      }
}
