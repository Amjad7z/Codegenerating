package com.wcarmon.codegen.model

import com.wcarmon.codegen.ast.EmptyExpression
import com.wcarmon.codegen.ast.Expression
import com.wcarmon.codegen.ast.FieldReadMode
import com.wcarmon.codegen.ast.FieldReadMode.DIRECT
import com.wcarmon.codegen.ast.RawLiteralExpression

/**
 * Configurable properties for generating PreparedStatement setters
 *
 * Properties independent of the Field/Column (consistent across fields in an entity)
 * See [java.sql.PreparedStatement]
 */
data class PreparedStatementBuilderConfig(

  /**
   * For the [FieldReadExpression]
   */
  val fieldReadMode: FieldReadMode = DIRECT,

  /**
   * For the [FieldReadExpression]
   */
  val fieldOwner: Expression = EmptyExpression,

  /**
   * Only for languages where compiler checks for null
   */
  val allowFieldNonNullAssertion: Boolean = true,

  val preparedStatementIdentifierExpression: Expression = RawLiteralExpression("ps"),
)
