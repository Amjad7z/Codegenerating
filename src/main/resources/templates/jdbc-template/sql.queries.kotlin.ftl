@file:JvmName("SQLQueries")
package ${request.packageName.value}
${request.jvmView.templateDebugInfo}

/* -------------------------------------------------------------------
 * SQL Queries
 *
 * Compatible with PostgreSQL, MariaDB, MySQL, Oracle, SQLite, DB2
 *
 * Useful for {@link java.sql.PreparedStatement}
 *   and {@link org.springframework.jdbc.core.JdbcTemplate}
 * -------------------------------------------------------------------
 */
<#list entities as entity>
  <#if entity.hasIdFields>
  /**
   * Find-by-PK
   * Entity: {@link ${entity.pkg.value}.${entity.name.upperCamel}}
   * PK column count: ${entity.idFields?size}
   * Columns count: ${entity.fields?size}
   */
  val SELECT_BY_PK__${entity.name.upperSnake} =
    """
    |SELECT ${entity.rdbmsView.commaSeparatedColumns}
    |FROM ${entity.rdbmsView.qualifiedTableName}
    |WHERE ${entity.rdbmsView.primaryKeyWhereClause_questionMarks}"
    """.trimMargin()

  /**
   * Test for existence of 1-row
   * Entity: {@link ${entity.pkg.value}.${entity.name.upperCamel}}
   * PK column count: ${entity.idFields?size}
   */
  val ROW_EXISTS__${entity.name.upperSnake} =
    """
    |SELECT COUNT(*)
    |FROM ${entity.rdbmsView.qualifiedTableName}
    |WHERE ${entity.rdbmsView.primaryKeyWhereClause_questionMarks}
    """.trimMargin()

<#if entity.hasNonIdFields>
  /**
   * Update 1-row
   * Entity: {@link ${entity.pkg.value}.${entity.name.upperCamel}}
   * PK column count: ${entity.idFields?size}
   * Columns count: ${entity.fields?size}
   */
  val UPDATE__${entity.name.upperSnake} =
    """
    |UPDATE ${entity.rdbmsView.qualifiedTableName}
    |SET
    |  ${entity.rdbmsView.updateSetClause_questionMarks}
    |WHERE ${entity.rdbmsView.primaryKeyWhereClause_questionMarks}
    """.trimMargin()

</#if>
  /**
   * Delete 1-row
   * Entity: {@link ${entity.pkg.value}.${entity.name.upperCamel}}
   * PK column count: ${entity.idFields?size}
   */
  val DELETE__${entity.name.upperSnake} =
    """
    |DELETE FROM ${entity.rdbmsView.qualifiedTableName}
    |WHERE ${entity.rdbmsView.primaryKeyWhereClause_questionMarks}
    """.trimMargin()
  </#if>

  /**
   * Select all rows
   * Entity: {@link ${entity.pkg.value}.${entity.name.upperCamel}}
   * Columns count: ${entity.fields?size}
   */
  val SELECT_ALL__${entity.name.upperSnake} =
    """
    |SELECT ${entity.rdbmsView.commaSeparatedColumns}
    |FROM ${entity.rdbmsView.qualifiedTableName}
    """.trimMargin()

  /**
   * Insert 1-row
   * Entity: {@link ${entity.pkg.value}.${entity.name.upperCamel}}
   * PK column count: ${entity.idFields?size}
   * Columns count: ${entity.fields?size}
   */
  val INSERT__${entity.name.upperSnake} =
    """
    |INSERT INTO ${entity.rdbmsView.qualifiedTableName} (
    |  ${entity.rdbmsView.commaSeparatedColumns}
    |)
    |VALUES (${entity.rdbmsView.questionMarkStringForInsert})
    """.trimMargin()

    ${entity.kotlinView.patchQueries()}

</#list>
