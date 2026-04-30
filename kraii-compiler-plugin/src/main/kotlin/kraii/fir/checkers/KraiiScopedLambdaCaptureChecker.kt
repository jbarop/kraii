package kraii.fir.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression

/**
 * Rejects code where a `@Scoped` local variable escapes its scope by
 * being captured in a lambda stored in a property.
 *
 * For example:
 * ```
 * @Scoped val resource = NoopResource()
 * val action: () -> Unit = { resource.close() }
 * ```
 * is rejected because the lambda captures a reference to `resource`,
 * allowing it to be called after the try-finally cleanup has already
 * closed the resource.
 */
object KraiiScopedLambdaCaptureChecker :
  FirPropertyChecker(MppCheckerKind.Common) {

  context(context: CheckerContext, reporter: DiagnosticReporter)
  override fun check(declaration: FirProperty) {
    val lambda = declaration.initializer as? FirAnonymousFunctionExpression
      ?: return

    if (lambdaCapturesScopedLocal(lambda, context.session)) {
      reporter.reportOn(
        declaration.source,
        KraiiErrors.SCOPED_MUST_NOT_ESCAPE,
        context,
      )
    }
  }
}
