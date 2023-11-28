package kraii.ir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class KraiiIrGenerationExtension: IrGenerationExtension {

  override fun generate(
    moduleFragment: IrModuleFragment,
    pluginContext: IrPluginContext,
  ) {
    KraiiClassLoweringPass(pluginContext).lower(moduleFragment)
  }

}
