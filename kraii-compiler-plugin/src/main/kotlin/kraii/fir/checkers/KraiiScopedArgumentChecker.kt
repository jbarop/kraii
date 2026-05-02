package kraii.fir.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol

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
 *
 * Lambda arguments to `inline` functions are not flagged because
 * they are inlined at the call site and don't capture variables
 * into a separate object. Parameters marked `noinline` or
 * `crossinline` are excluded from this exemption.
 */
object KraiiScopedArgumentChecker :
  FirFunctionCallChecker(MppCheckerKind.Common) {

  context(context: CheckerContext, reporter: DiagnosticReporter)
  override fun check(expression: FirFunctionCall) {
    val calledFunction =
      expression.calleeReference.toResolvedFunctionSymbol()
    val isInlineCall = calledFunction?.isInline == true

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

      // Lambda arguments to inline functions are inlined at the
      // call site, so they don't actually capture variables.
      // Skip them unless the corresponding parameter is noinline
      // or crossinline.
      if (isInlineCall &&
        isInlinedLambdaParameter(expression, lambda)
      ) {
        continue
      }

      if (lambdaCapturesScopedLocal(lambda, context.session)) {
        reporter.reportOn(
          argument.source,
          KraiiErrors.SCOPED_MUST_NOT_ESCAPE,
          context,
        )
      }
    }
  }

  /**
   * Returns `true` if the given lambda argument corresponds to a
   * parameter that will actually be inlined (not marked `noinline`
   * or `crossinline`).
   */
  private fun isInlinedLambdaParameter(
    call: FirFunctionCall,
    lambda: FirAnonymousFunctionExpression,
  ): Boolean {
    val function =
      call.calleeReference.toResolvedFunctionSymbol()
        ?: return false
    val argumentIndex =
      call.argumentList.arguments.indexOf(lambda)
    if (argumentIndex < 0) return false
    val parameter =
      function.valueParameterSymbols.getOrNull(argumentIndex)
        ?: return false
    return !parameter.isNoinline && !parameter.isCrossinline
  }
}
