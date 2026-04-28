package kraii.fir.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment

/**
 * Rejects code where a `@Scoped` local variable escapes its scope
 * by being assigned to another variable (e.g. `outer = resource`).
 *
 * This catches reassignment-based escapes that the property-initializer
 * checker (`KraiiScopedEscapeChecker`) does not cover, such as
 * assigning a `@Scoped` local to an outer `var` captured by a lambda.
 */
object KraiiScopedAssignmentChecker :
  FirExpressionChecker<FirVariableAssignment>(MppCheckerKind.Common) {

  context(context: CheckerContext, reporter: DiagnosticReporter)
  override fun check(expression: FirVariableAssignment) {
    val rValue = expression.rValue

    if (!referencesScopedLocal(rValue, context.session)) return

    reporter.reportOn(
      rValue.source,
      KraiiErrors.SCOPED_MUST_NOT_ESCAPE,
      context,
    )
  }
}
