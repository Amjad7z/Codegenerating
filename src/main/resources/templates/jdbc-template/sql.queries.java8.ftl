package ${request.packageName.value};

/**
 * SQL Queries
 * <p>
 * Compatible with PostgreSQL, MariaDB, MySQL, Oracle, SQLite, DB2
 * <p>
 * Useful for {@link java.sql.PreparedStatement} and {@link org.springframework.jdbc.core.JdbcTemplate}
 * <p>
 * See: ${request.prettyTemplateName}
 */
public final class SQLQueries {
<#list entities as entity>

  <#if entity.hasPrimaryKeyFields>
  /**
   * Find-by-PK
   * Entity: {@link ${entity.pkg.value}.${entity.name.upperCamel}}
   * PK column count: ${entity.primaryKeyFields?size}
   * Columns count: ${entity.fields?size}
   */
  public static final String SELECT_BY_PK__${entity.name.upperSnake} =
    "SELECT ${entity.commaSeparatedColumns}"
    + " FROM \"${entity.name.lowerSnake}\""
    + " WHERE ${entity.pkWhereClause}";

  /**
   * Test for existence of 1-row
   * Entity: {@link ${entity.pkg.value}.${entity.name.upperCamel}}
   * PK column count: ${entity.primaryKeyFields?size}
   */
  public static final String ROW_EXISTS__${entity.name.upperSnake} =
    "SELECT COUNT(*)"
    + " FROM \"${entity.name.lowerSnake}\""
    + " WHERE ${entity.pkWhereClause}";

  /**
   * Update 1-row
   * Entity: {@link ${entity.pkg.value}.${entity.name.upperCamel}}
   * PK column count: ${entity.primaryKeyFields?size}
   * Columns count: ${entity.fields?size}
   */
  public static final String UPDATE__${entity.name.upperSnake} =
    "UPDATE \"${entity.name.lowerSnake}\""
    + " SET"
    + " ${entity.updateSetClause}"
    + " WHERE ${entity.pkWhereClause}";

  /**
   * Delete 1-row
   * Entity: {@link ${entity.pkg.value}.${entity.name.upperCamel}}
   * PK column count: ${entity.primaryKeyFields?size}
   */
  public static final String DELETE__${entity.name.upperSnake} =
    "DELETE FROM \"${entity.name.lowerSnake}\""
    + " WHERE ${entity.pkWhereClause}";
  </#if>

  /**
   * Select all rows
   * Entity: {@link ${entity.pkg.value}.${entity.name.upperCamel}}
   * Columns count: ${entity.fields?size}
   */
  public static final String SELECT_ALL__${entity.name.upperSnake} =
    "SELECT ${entity.commaSeparatedColumns}"
    + " FROM \"${entity.name.lowerSnake}\"";

  /**
   * Insert 1-row
   * Entity: {@link ${entity.pkg.value}.${entity.name.upperCamel}}
   * PK column count: ${entity.primaryKeyFields?size}
   * Columns count: ${entity.fields?size}
   */
  public static final String INSERT__${entity.name.upperSnake} =
    "INSERT INTO \"${entity.name.lowerSnake}\""
    + " (${entity.commaSeparatedColumns})"
    + " VALUES (${entity.questionMarkStringForInsert})";

  <#if entity.hasPrimaryKeyFields>
    <#list entity.nonPrimaryKeyFields as field>
      public static final String PATCH__${entity.name.upperSnake}__${field.name.upperSnake} =
        "UPDATE \"${entity.name.lowerSnake}\""
        + " SET ${field.name.lowerSnake}=?"
        + " WHERE ${entity.pkWhereClause}";

    </#list>
  </#if>
</#list>
}
