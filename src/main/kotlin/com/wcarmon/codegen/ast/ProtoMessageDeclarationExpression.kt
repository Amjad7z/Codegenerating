package com.wcarmon.codegen.ast

import com.wcarmon.codegen.ast.ProtoFieldDeclarationExpression.Companion.NUMBER_COMPARATOR
import com.wcarmon.codegen.model.Name
import com.wcarmon.codegen.model.TargetLanguage

/**
 * See https://developers.google.com/protocol-buffers/docs/proto3#simple
 */
data class ProtoMessageDeclarationExpression(
  private val name: Name,

  private val enums: List<ProtoEnumDeclarationExpression> = listOf(),
  private val fields: Collection<ProtoFieldDeclarationExpression> = listOf(),

  //TODO: support reserved fields
  // https://developers.google.com/protocol-buffers/docs/proto3#reserved
) : Expression {

  init {

    val fieldNumbers = fields.map { it.number }
    require(fieldNumbers.toSet().size == fieldNumbers.size) {
      "fieldNumbers must be unique: $fields"
    }
  }

  private val sortedFieldExpressions: List<ProtoFieldDeclarationExpression> =
    fields.sortedWith(NUMBER_COMPARATOR)

  override fun render(
    targetLanguage: TargetLanguage,
    terminate: Boolean,
    lineIndentation: String,
  ): String {
    check(targetLanguage.isProtobuf)

    //TODO: documentation on top
    return """
    |message ${name.upperCamel}{
    ${renderFields(targetLanguage)}
    |  
    ${renderEnums(targetLanguage)}  
    |}
    """.trimMargin()

    //TODO: prefix every lines with lineIndentation
  }

  private fun renderFields(targetLanguage: TargetLanguage): String =
    sortedFieldExpressions.joinToString("\n") {
      "|  ${it.render(targetLanguage, true)}"
    }

  private fun renderEnums(targetLanguage: TargetLanguage): String =
    enums.joinToString("\n") {
      "|  ${it.render(targetLanguage)}"
    }
}
