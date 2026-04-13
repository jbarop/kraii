package kraii.fir.checkers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

class KraiiCheckersExtension(
  session: FirSession,
) : FirAdditionalCheckersExtension(session) {

  override val declarationCheckers: DeclarationCheckers =
    object : DeclarationCheckers() {
      override val propertyCheckers: Set<FirPropertyChecker> =
        setOf(KraiiScopedPropertyChecker)
    }
}
