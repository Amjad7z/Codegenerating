package com.wcarmon.codegen.ast

import com.wcarmon.codegen.model.Field
import com.wcarmon.codegen.model.ProtoFieldNumber
import com.wcarmon.codegen.model.TargetLanguage
import com.wcarmon.codegen.util.effectiveProtobufType

//TODO: document me
data class ProtoFieldDeclarationExpression(
  private val field: Field,
  val number: ProtoFieldNumber,

  private val deprecated: Boolean = false,
  private val repeated: Boolean = false,

  //TODO: DocumentationExpression
) : Expression {

  override fun render(
    targetLanguage: TargetLanguage,
    terminate: Boolean,
  ): String {
    check(targetLanguage.isProtobuf)
    check(terminate)

    val repeatedPrefix =
      if (repeated) "repeated "
      else ""

    val deprecatedSegment =
      if (deprecated) " [deprecated = true]"
      else ""

    return "${repeatedPrefix}${effectiveProtobufType(field)} ${field.name.lowerSnake} " +
        "= ${number.value}${deprecatedSegment};"
  }
}
