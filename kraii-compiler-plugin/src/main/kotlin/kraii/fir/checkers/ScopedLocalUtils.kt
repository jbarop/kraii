package kraii.fir.checkers

import kraii.scopedClassId
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol

/**
 * Returns `true` if the given expression is a direct reference to a
 * `@Scoped` local variable.
 */
fun referencesScopedLocal(
  expression: FirExpression,
  session: FirSession,
): Boolean {
  if (expression !is FirPropertyAccessExpression) return false

  val symbol = expression.calleeReference.toResolvedPropertySymbol()
    ?: return false

  return symbol.isLocal &&
    symbol.hasAnnotation(scopedClassId, session)
}
