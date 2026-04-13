package kraii.fir

import kraii.fir.checkers.KraiiCheckersExtension
import kraii.fir.checkers.KraiiErrors
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

/**
 * Registers FIR extensions.
 *
 * - `close()` method generation for `AutoCloseable` classes
 * - `@Scoped` usage validation (checkers and diagnostics)
 */
class KraiiFirExtensionRegistrar : FirExtensionRegistrar() {

  override fun ExtensionRegistrarContext.configurePlugin() {
    +::KraiiAddCloseMethodExtension
    +::KraiiCheckersExtension
    registerDiagnosticContainers(KraiiErrors)
  }
}
