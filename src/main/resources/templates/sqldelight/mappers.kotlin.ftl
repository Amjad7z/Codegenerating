@file:JvmName("SQLDelightMappers")
package ${request.packageName.value}
${request.jvmView.templateDebugInfo}

${request.kotlinView.serializeImports(
request.extraJVMImports,
request.jvmContextClass)}

<#list entities as entity>
${entity.sqlDelightView.modelToRecord}

@Suppress("UNCHECKED_CAST")
${entity.sqlDelightView.recordToModel}

</#list>
