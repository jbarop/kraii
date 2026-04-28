package kraii.fir.checkers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirReturnExpressionChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment

class KraiiCheckersExtension(
  session: FirSession,
) : FirAdditionalCheckersExtension(session) {

  override val declarationCheckers: DeclarationCheckers =
    object : DeclarationCheckers() {
      override val propertyCheckers: Set<FirPropertyChecker> =
        setOf(KraiiScopedPropertyChecker, KraiiScopedEscapeChecker)
    }

  override val expressionCheckers: ExpressionCheckers =
    object : ExpressionCheckers() {
      override val returnExpressionCheckers:
        Set<FirReturnExpressionChecker> =
        setOf(KraiiScopedReturnChecker)

      override val variableAssignmentCheckers:
        Set<FirExpressionChecker<FirVariableAssignment>> =
        setOf(KraiiScopedAssignmentChecker)

      override val functionCallCheckers:
        Set<FirFunctionCallChecker> =
        setOf(KraiiScopedArgumentChecker)
    }
}
