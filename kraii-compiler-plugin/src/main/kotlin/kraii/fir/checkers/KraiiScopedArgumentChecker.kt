package kraii.fir.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall

/**
 * Rejects code where a `@Scoped` local variable escapes its scope
 * by being passed as an argument to a function, method, or
 * constructor call.
 *
 * Catches two escape patterns:
 * - Direct argument passing: `consume(resource)` or
 *   `list.add(resource)` where `resource` is `@Scoped`.
 * - Lambda argument capture: `lazy { resource }` or
 *   `execute { resource.close() }` where the lambda captures a
 *   reference to a `@Scoped` local.
 */
object KraiiScopedArgumentChecker :
  FirFunctionCallChecker(MppCheckerKind.Common) {

  context(context: CheckerContext, reporter: DiagnosticReporter)
  override fun check(expression: FirFunctionCall) {
    for (argument in expression.argumentList.arguments) {
      if (referencesScopedLocal(argument, context.session)) {
        reporter.reportOn(
          argument.source,
          KraiiErrors.SCOPED_MUST_NOT_ESCAPE,
          context,
        )
        continue
      }

      val lambda =
        argument as? FirAnonymousFunctionExpression ?: continue
      if (lambdaCapturesScopedLocal(lambda, context.session)) {
        reporter.reportOn(
          argument.source,
          KraiiErrors.SCOPED_MUST_NOT_ESCAPE,
          context,
        )
      }
    }
  }
}
