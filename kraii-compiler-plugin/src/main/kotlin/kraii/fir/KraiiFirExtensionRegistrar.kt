package kraii.fir

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

/**
 * Registers the FIR extension that generates `close()` method stubs.
 */
class KraiiFirExtensionRegistrar : FirExtensionRegistrar() {

  override fun ExtensionRegistrarContext.configurePlugin() {
    +::KraiiAddCloseMethodExtension
  }
}
