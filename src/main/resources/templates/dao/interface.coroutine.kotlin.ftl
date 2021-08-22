package ${request.packageName.value}

<#list request.extraJVMImports as importable>
import ${importable}
</#list>
<#list entity.javaImportsForFields as importable>
import ${importable}
</#list>


/**
 * DAO Contract for [${entity.pkg.value}.${entity.name.upperCamel}]
 * PK field count: ${entity.primaryKeyFields?size}
 * Field count: ${entity.fields?size}
 *
 * Assumes coroutines & context passed thru coroutineContext.
 * Implementations must be ThreadSafe
 * See: ${request.prettyTemplateName}
 */
@Suppress("TooManyFunctions")
interface ${entity.name.upperCamel}DAO {

<#if entity.hasPrimaryKeyFields>
 /**
  * Delete at-most-one existing {@link ${entity.pkg.value}.${entity.name.upperCamel}} instance
  *
  * NOOP if no matching entity exists
  *
  * @param TODO
  */
  suspend fun delete(${entity.kotlinMethodArgsForPKFields(false)})

  /**
   * @param TODO
   * @return true when {@link ${entity.pkg.value}.${entity.name.upperCamel}} exists with matching PK
   */
  suspend fun exists(${entity.kotlinMethodArgsForPKFields(false)}): Boolean

  /**
   * @param TODO
   * @return one {@link ${entity.pkg.value}.${entity.name.upperCamel}} instance (matching PKs) or null
   */
  suspend fun findById( ${entity.kotlinMethodArgsForPKFields(false)}): ${entity.name.upperCamel}?
</#if>

  /**
   * Create at-most-one {@link ${entity.pkg.value}.${entity.name.upperCamel}} instance
   */
  suspend fun create(entity: ${entity.name.upperCamel})

  /**
   * @return all {@link ${entity.pkg.value}.${entity.name.upperCamel}} entities or empty List (never null)
   */
  suspend fun list(): List<${entity.name.upperCamel}>

  /**
   * Update all (non-PK) fields on one {@link ${entity.pkg.value}.${entity.name.upperCamel}} instance
   * (${entity.nonPrimaryKeyFields?size} non-PK fields)
   */
  suspend fun update(entity: ${entity.name.upperCamel})

  /**
   * Upsert/Put {@link ${entity.pkg.value}.${entity.name.upperCamel}}
   *
   * Update if entity exists, Create if entity does not exist
   *
   * Same concept as {@link java.util.Map#put}
   *
   * @param entity to update or create
   */
  suspend fun upsert(entity: ${entity.name.upperCamel})

<#list entity.nonPrimaryKeyFields as field>
  /**
   * Patch/Set
   *
   * Set one field: {@link ${entity.pkg.value}.${entity.name.upperCamel}#${field.name.lowerCamel}}
   *
   * @param ${field.name.lowerCamel} - replacement for existing value
   */
  suspend fun set${field.name.upperCamel}(
    ${entity.kotlinMethodArgsForPKFields(false)},
    ${field.name.lowerCamel}: ${field.unqualifiedKotlinType}
  )

</#list>
  }