package kraii.fir.checkers

import kraii.scopedClassId
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirElvisExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol

/**
 * Returns `true` if the given expression references a `@Scoped` local
 * variable, either directly or through transparent wrapper expressions
 * like `if`/`when` branches, elvis operators, type casts, and blocks.
 */
fun referencesScopedLocal(
  expression: FirExpression,
  session: FirSession,
): Boolean =
  when (expression) {
    is FirPropertyAccessExpression -> {
      val symbol =
        expression.calleeReference.toResolvedPropertySymbol()
      symbol != null &&
        symbol.isLocal &&
        symbol.hasAnnotation(scopedClassId, session)
    }

    is FirBlock -> {
      val last = expression.statements.lastOrNull()
      last is FirExpression &&
        referencesScopedLocal(last, session)
    }

    is FirWhenExpression ->
      expression.branches.any {
        referencesScopedLocal(it.result, session)
      }

    is FirElvisExpression ->
      referencesScopedLocal(expression.lhs, session) ||
        referencesScopedLocal(expression.rhs, session)

    is FirTypeOperatorCall ->
      expression.argumentList.arguments.any {
        referencesScopedLocal(it, session)
      }

    else -> false
  }
