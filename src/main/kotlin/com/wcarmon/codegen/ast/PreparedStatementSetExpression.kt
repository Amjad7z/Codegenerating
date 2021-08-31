package com.wcarmon.codegen.ast

import com.wcarmon.codegen.model.Field
import com.wcarmon.codegen.model.JDBCColumnIndex
import com.wcarmon.codegen.model.Name
import java.sql.JDBCType

/**
 * For nullable fields:
 * - Combines [PreparedStatementNonNullSetExpression] and [PreparedStatementNullSetExpression]
 * - Wraps in a conditional
 *
 * For non-nullable fields
 * - Same as [PreparedStatementNonNullSetExpression]
 */
data class PreparedStatementSetExpression(
  private val columnIndex: JDBCColumnIndex,
  private val columnType: JDBCType,
  private val field: Field,
  private val fieldReadExpression: Expression,
  private val preparedStatementIdentifierExpression: Expression = RawExpression("ps"),
  private val setterMethod: Name,
) : Expression {

  override val expressionName = PreparedStatementSetExpression::class.java.simpleName

  private val nonNullExpression: PreparedStatementNonNullSetExpression =
    PreparedStatementNonNullSetExpression(
      columnIndex = columnIndex,
      fieldReadExpression = fieldReadExpression,
      preparedStatementIdentifierExpression = preparedStatementIdentifierExpression,
      setterMethod = setterMethod,
    )

  private val nullExpression: PreparedStatementNullSetExpression =
    PreparedStatementNullSetExpression(
      columnIndex = columnIndex,
      columnType = columnType,
      preparedStatementIdentifierExpression = preparedStatementIdentifierExpression,
    )

  private val conditionalExpression: ConditionalExpression =
    ConditionalExpression(
      condition = NullComparisonExpression(
        compareToMe = fieldReadExpression,
      ),
      expressionForFalse = nonNullExpression,
      expressionForTrue = nullExpression,
    )


  override fun renderWithoutDebugComments(
    config: RenderConfig,
  ) =
    if (field.type.nullable) {
      conditionalExpression

    } else {
      nonNullExpression
    }
      .render(config.unterminated)
}
