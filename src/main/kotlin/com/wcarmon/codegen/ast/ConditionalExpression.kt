package com.wcarmon.codegen.ast

import com.wcarmon.codegen.model.TargetLanguage
import com.wcarmon.codegen.model.TargetLanguage.*

/**
 * If or If-Else expression
 *
 * For Java: a Conditional Statement
 * For Kotlin: a Conditional Expression
 */
data class ConditionalExpression(
  private val condition: Expression,
  private val expressionForTrue: Expression,
  private val expressionForFalse: Expression = EmptyExpression,
) : Expression {

  override fun render(
    targetLanguage: TargetLanguage,
    terminate: Boolean,
    lineIndentation: String,
  ) = when (targetLanguage) {
    C_17,
    CPP_14,
    CPP_17,
    CPP_20,
    DART_2,
    JAVA_08,
    JAVA_11,
    JAVA_17,
    TYPESCRIPT_4,
    -> cStyle(targetLanguage, terminate, lineIndentation)

    KOTLIN_JVM_1_4,
    -> cStyle(targetLanguage, false, lineIndentation)

    GOLANG_1_7,
    RUST_1_54,
    SWIFT_5,
    -> noParens(targetLanguage, false, lineIndentation)

    PYTHON_3 -> pythonStyle(targetLanguage, lineIndentation)

    PROTOCOL_BUFFERS_3,
    -> TODO("Conditionals not supported on $targetLanguage")

    else -> TODO("Conditionals not supported on $targetLanguage")
  }

  //TODO: use lineIndentation
  private fun cStyle(
    targetLanguage: TargetLanguage,
    terminate: Boolean = false,
    lineIndentation: String,
  ): String = if (expressionForFalse.isBlank(targetLanguage)) {
    """
      |if (${condition.render(targetLanguage, false)}) {
      |  ${expressionForTrue.render(targetLanguage, terminate)}   
      |}
      |
      """

  } else {
    """
      |if (${condition.render(targetLanguage, false)}) {
      |  ${expressionForTrue.render(targetLanguage, terminate)}   
      |} else {
      |  ${expressionForFalse.render(targetLanguage, terminate)}
      |}
      |
      """
  }.trimMargin()

  private fun noParens(
    targetLanguage: TargetLanguage,
    terminate: Boolean = false,
    lineIndentation: String,
  ): String = if (expressionForFalse.isBlank(targetLanguage)) {
    """
      |if ${condition.render(targetLanguage, false)} {
      |  ${expressionForTrue.render(targetLanguage, terminate)}
      |}
      |
      """
  } else {
    """
      |if ${condition.render(targetLanguage, false)} {
      |  ${expressionForTrue.render(targetLanguage, terminate)}
      |} else {
      |  ${expressionForFalse.render(targetLanguage, terminate)}
      |}
      |
      """
  }.trimMargin()

  private fun pythonStyle(
    targetLanguage: TargetLanguage,
    lineIndentation: String,
  ) =
    if (expressionForFalse.isBlank(targetLanguage)) {
      """
      |if ${condition.render(targetLanguage, false)}:
      |  ${expressionForTrue.render(targetLanguage, false)}
      |
      """
    } else {
      """
      |if ${condition.render(targetLanguage, false)}:
      |  ${expressionForTrue.render(targetLanguage, false)}
      |else:          
      |  ${expressionForFalse.render(targetLanguage, false)}
      |
      """
    }.trimMargin()
}
