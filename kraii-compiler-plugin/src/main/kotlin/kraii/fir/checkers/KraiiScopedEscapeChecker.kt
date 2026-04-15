package kraii.fir.checkers

import kraii.scopedClassId
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

/**
 * Rejects code where a `@Scoped` local variable escapes its scope
 * by being assigned to another local variable.
 *
 * For example, `val alias = resource` where `resource` is `@Scoped`
 * is rejected because the alias would outlive the try-finally cleanup
 * generated for the original variable. This also applies when the
 * alias itself is `@Scoped`, since the same object would be closed
 * twice.
 */
object KraiiScopedEscapeChecker :
  FirPropertyChecker(MppCheckerKind.Common) {

  context(context: CheckerContext, reporter: DiagnosticReporter)
  override fun check(declaration: FirProperty) {
    val initializer = declaration.initializer ?: return

    if (!referencesScopedLocal(initializer, context.session)) return

    val error =
      if (declaration.hasAnnotation(scopedClassId, context.session)) {
        KraiiErrors.SCOPED_MUST_NOT_ALIAS
      } else {
        KraiiErrors.SCOPED_MUST_NOT_ESCAPE
      }

    reporter.reportOn(initializer.source, error, context)
  }
}

/**
 * Returns `true` if the given expression is a direct reference to a
 * `@Scoped` local variable.
 */
private fun referencesScopedLocal(
  expression: FirExpression,
  session: FirSession,
): Boolean {
  if (expression !is FirPropertyAccessExpression) return false

  val symbol = expression.calleeReference.toResolvedPropertySymbol()
    ?: return false

  return symbol.isLocal &&
    symbol.hasAnnotation(scopedClassId, session)
}
