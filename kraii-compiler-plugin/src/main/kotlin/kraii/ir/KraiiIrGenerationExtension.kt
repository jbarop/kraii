package kraii.ir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

class KraiiIrGenerationExtension : IrGenerationExtension {

  override fun generate(
    moduleFragment: IrModuleFragment,
    pluginContext: IrPluginContext,
  ) {
    moduleFragment.acceptChildrenVoid(KraiiCloseMethodBodyGenerator(pluginContext))
  }

}
