package kraii.fir.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirReturnExpressionChecker
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression

/**
 * Rejects code where a `@Scoped` local variable escapes its scope
 * by being returned from a function.
 *
 * For example, `return resource` where `resource` is `@Scoped` is
 * rejected because the caller would receive a reference to an object
 * that has already been closed by the try-finally cleanup generated
 * for the `@Scoped` variable.
 */
object KraiiScopedReturnChecker :
  FirReturnExpressionChecker(MppCheckerKind.Common) {

  context(context: CheckerContext, reporter: DiagnosticReporter)
  override fun check(expression: FirReturnExpression) {
    val result = expression.result

    if (!referencesScopedLocal(result, context.session)) return

    reporter.reportOn(
      result.source,
      KraiiErrors.SCOPED_MUST_NOT_ESCAPE,
      context,
    )
  }
}
