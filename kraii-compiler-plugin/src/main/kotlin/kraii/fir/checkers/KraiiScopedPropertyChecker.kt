package kraii.fir.checkers

import kraii.scopedClassId
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.hasAnnotation

/**
 * Validates that `@Scoped` is only used on `val` declarations, not `var`.
 *
 * This checker runs during the FIR phase and reports a compile error if
 * `@Scoped` is applied to a mutable property or local variable.
 */
object KraiiScopedPropertyChecker :
  FirPropertyChecker(MppCheckerKind.Common) {

  context(context: CheckerContext, reporter: DiagnosticReporter)
  override fun check(declaration: FirProperty) {
    if (!declaration.hasAnnotation(scopedClassId, context.session)) {
      return
    }

    if (declaration.isVar) {
      reporter.reportOn(
        declaration.source,
        KraiiErrors.SCOPED_MUST_BE_VAL,
        context,
      )
    }
  }
}
