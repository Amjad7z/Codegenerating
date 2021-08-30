package com.wcarmon.codegen.ast

import com.wcarmon.codegen.model.TargetLanguage
import com.wcarmon.codegen.model.TargetLanguage.*

data class MethodDeclarationExpression(
  private val header: MethodHeaderExpression,

  private val extraPreconditions: List<PreconditionExpression> = listOf(),

  private val body: List<Expression>,
) : Expression {

  override fun render(
    targetLanguage: TargetLanguage,
    terminate: Boolean,
    lineIndentation: String,
  ): String {
    return when (targetLanguage) {
      C_17 -> TODO()
      CPP_14,
      CPP_17,
      CPP_20,
      -> TODO()

      DART_2 -> TODO()

      GOLANG_1_7 -> TODO()

      JAVA_08,
      JAVA_11,
      JAVA_17,
      -> handleJava()

      KOTLIN_JVM_1_4 -> handleKotlin()

      PROTOCOL_BUFFERS_3 -> TODO()

      PYTHON_3 -> TODO()

      RUST_1_54 -> TODO()


      SWIFT_5 -> TODO()

      TYPESCRIPT_4 -> TODO()

      SQL_DB2 -> TODO()
      SQL_H2 -> TODO()
      SQL_MARIA -> TODO()
      SQL_MYSQL -> TODO()
      SQL_ORACLE -> TODO()
      SQL_POSTGRESQL -> TODO()
      SQL_SQLITE -> TODO()
    }
  }

  private fun handleJava(): String {
    TODO("Not yet implemented")
  }

  private fun handleKotlin(): String {
    TODO("Not yet implemented")
  }
}


