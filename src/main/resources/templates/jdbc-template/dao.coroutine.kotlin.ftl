package ${request.packageName.value}

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.jdbc.core.RowMapper
<#if request.jvmContextClass?has_content>
import ${request.jvmContextClass}
</#if>
<#list entity.kotlinView.importsForFields as importable>
import ${importable}
</#list>
<#list request.extraJVMImports as importable>
import ${importable}
</#list>
<#if entity.jvmView.requiresObjectWriter>
import com.fasterxml.jackson.databind.ObjectWriter
</#if>
import java.sql.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * DAO implementation for [${entity.pkg.value}.${entity.name.upperCamel}].
 * Uses [org.springframework.jdbc.core.JdbcTemplate] to execute queries.
 * Uses [java.sql.PreparedStatement]
 *
 * See https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/core/JdbcTemplate.html
 * Threadsafe & Reusable after construction
 */
@Suppress("MagicNumber", "TooManyFunctions") // column & placeholder numbers
class ${entity.name.upperCamel}DAOImpl(
  private val jdbcTemplate: JdbcTemplate,

<#if entity.jvmView.requiresObjectWriter>
/** To serialize collection fields */
private val objectWriter: ObjectWriter,
</#if>

private val rowMapper: RowMapper<${entity.name.upperCamel}>,

) : ${entity.name.upperCamel}DAO {

<#if entity.hasIdFields>
  override suspend fun delete(${entity.kotlinView.methodArgsForIdFields(false)}): Unit = withContext(Dispatchers.IO) {
    //TODO: kotlin preconditions on PK fields (see FieldValidation)

    jdbcTemplate.update(DELETE__${entity.name.upperSnake}) { ps ->
        ${entity.kotlinView.preparedStatementSetterStatementsForPK}
    }
  }

  override suspend fun exists(${entity.kotlinView.methodArgsForIdFields(false)}): Boolean = withContext(Dispatchers.IO) {
    //TODO: kotlin preconditions on PK fields (see FieldValidation)

    1 == jdbcTemplate.queryForObject(
      ROW_EXISTS__${entity.name.upperSnake},
      Int::class.java,
      ${entity.rdbmsView.jdbcSerializedPrimaryKeyFields})
  }

  override suspend fun findById(${entity.kotlinView.methodArgsForIdFields(false)}): ${entity.name.upperCamel}? = withContext(Dispatchers.IO) {
    //TODO: kotlin preconditions on PK fields (see FieldValidation)

    val results =
      jdbcTemplate.query(
        SELECT_BY_PK__${entity.name.upperSnake},
        PreparedStatementSetter { ps ->
          ${entity.kotlinView.preparedStatementSetterStatementsForPK}
        },
        rowMapper)

    if (results.isEmpty()) {
      return@withContext null
    }

    if (results.size > 1) {
      //TODO: include PK arg(s)
      throw IllegalStateException("Multiple rows match the PK: context=${r"$"}coroutineContext, results=${r"$"}results")
    }

    results[0]
  }
</#if>

  override suspend fun create(entity: ${entity.name.upperCamel}): Unit = withContext(Dispatchers.IO) {

    val affectedRowCount = jdbcTemplate.update(
      INSERT__${entity.name.upperSnake},
    ) { ps ->
        ${entity.kotlinView.insertPreparedStatementSetterStatements}
      }

    check(affectedRowCount == 1){ "1-row should have been inserted for entity=${r"$"}entity" }
  }

  override suspend fun list(): List<${entity.name.upperCamel}> = withContext(Dispatchers.IO) {
    jdbcTemplate.query(
      SELECT_ALL__${entity.name.upperSnake},
      rowMapper
    )
  }

  override suspend fun update(entity: ${entity.name.upperCamel}): Unit = withContext(Dispatchers.IO) {

   jdbcTemplate.update(
      UPDATE__${entity.name.upperSnake}
    ) { ps ->
        ${entity.kotlinView.updatePreparedStatementSetterStatements}
    }
  }

  override suspend fun upsert(entity: ${entity.name.upperCamel}): Unit = withContext(Dispatchers.IO) {
    TODO("finish this method")
  }

  // -- Patch methods
<#list entity.nonIdFields as field>
  override suspend fun set${field.name.upperCamel}(
    ${entity.kotlinView.methodArgsForIdFields(false)},
    ${field.name.lowerCamel}: ${field.kotlinView.unqualifiedType}): Unit = withContext(Dispatchers.IO) {

    //TODO: '${field.name.lowerCamel}' field validation here (since not part of the POJO validation)
      jdbcTemplate.update(
        PATCH__${entity.name.upperSnake}__${field.name.upperSnake}
      ) { ps ->
        ${field.kotlinView.updateFieldPreparedStatementSetterStatements(entity.idFields)}
      }
  }

</#list>
}
