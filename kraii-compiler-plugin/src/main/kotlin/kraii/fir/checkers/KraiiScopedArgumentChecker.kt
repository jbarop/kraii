package kraii.fir.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall

/**
 * Rejects code where a `@Scoped` local variable escapes its scope
 * by being passed as an argument to a function, method, or
 * constructor call.
 *
 * For example, `consume(resource)` or `list.add(resource)` where
 * `resource` is `@Scoped` is rejected because the callee could
 * store a reference to an object that will be closed by the
 * try-finally cleanup generated for the `@Scoped` variable.
 */
object KraiiScopedArgumentChecker :
  FirFunctionCallChecker(MppCheckerKind.Common) {

  context(context: CheckerContext, reporter: DiagnosticReporter)
  override fun check(expression: FirFunctionCall) {
    for (argument in expression.argumentList.arguments) {
      if (!referencesScopedLocal(argument, context.session)) continue

      reporter.reportOn(
        argument.source,
        KraiiErrors.SCOPED_MUST_NOT_ESCAPE,
        context,
      )
    }
  }
}
