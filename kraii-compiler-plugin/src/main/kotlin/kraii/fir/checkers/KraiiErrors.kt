package kraii.fir.checkers

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.psi.KtElement

object KraiiErrors : KtDiagnosticsContainer() {

  val SCOPED_MUST_BE_VAL by error0<KtElement>()
  val SCOPED_MUST_NOT_ESCAPE by error0<KtElement>()
  val SCOPED_MUST_NOT_ALIAS by error0<KtElement>()

  override fun getRendererFactory(): BaseDiagnosticRendererFactory =
    KraiiErrorMessages
}

private object KraiiErrorMessages : BaseDiagnosticRendererFactory() {

  @Suppress("PropertyName")
  override val MAP by KtDiagnosticFactoryToRendererMap("Kraii") {
    it.put(
      KraiiErrors.SCOPED_MUST_BE_VAL,
      "@Scoped must be 'val', not 'var'.",
    )
    it.put(
      KraiiErrors.SCOPED_MUST_NOT_ESCAPE,
      "@Scoped variable must not escape its scope.",
    )
    it.put(
      KraiiErrors.SCOPED_MUST_NOT_ALIAS,
      "@Scoped variable must not be initialized from another @Scoped variable.",
    )
  }
}
