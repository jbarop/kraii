package kraii.ir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class KraiiIrGenerationExtension: IrGenerationExtension {

  override fun generate(
    moduleFragment: IrModuleFragment,
    pluginContext: IrPluginContext,
  ) {
    val pass = KraiiClassLoweringPass(pluginContext)
    for (file in moduleFragment.files) {
      pass.runOnFilePostfix(file)
    }
  }

}
