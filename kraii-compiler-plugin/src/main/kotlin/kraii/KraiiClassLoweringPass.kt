package kraii

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.FqName

/**
 * Generates the implementation of [AutoCloseable.close].
 */
class KraiiClassLoweringPass(
  private val pluginContext: IrPluginContext,
) : ClassLoweringPass {

  override fun lower(irClass: IrClass) {
    if (!irClass.toIrBasedDescriptor().implementsAutoClosable()) {
      return
    }

    val funPrintln = pluginContext.referenceFunctions(FqName("kotlin.io.println"))
      .single {
        val parameters = it.owner.valueParameters
        parameters.size == 1 && parameters[0].type == pluginContext.irBuiltIns.anyNType
      }

    val closeFunction = irClass.functions.single { it.name == closeName }
    closeFunction.body = DeclarationIrBuilder(pluginContext, closeFunction.symbol).irBlockBody {
      val callPrintln = irCall(funPrintln)
      callPrintln.putValueArgument(0, irString("Hello World from Compiler Plugin!"))
      +callPrintln
    }
  }

}
