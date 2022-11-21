package kraii

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.GET_PROPERTY
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.superTypes

/**
 * Generates the implementation of [AutoCloseable.close].
 */
class KraiiClassLoweringPass(
  private val pluginContext: IrPluginContext,
) : ClassLoweringPass {

  override fun lower(irClass: IrClass) {
    if (!irClass.toIrBasedDescriptor().implementsAutoClosable()) return
    val thisCloseFunction = irClass.functions.single { it.name == closeName }
    if (thisCloseFunction.body != null) return
    val propertiesToClose = irClass.properties
      .filter { it.instanceOfAutoClosable() }

    thisCloseFunction.irBlockBody {
      propertiesToClose.forEach { propertyToClose ->
        +irCall(propertyToClose.closeFunction).apply {
          dispatchReceiver = irCall(propertyToClose.getter!!, origin = GET_PROPERTY).apply {
            dispatchReceiver = irGet(thisCloseFunction.dispatchReceiverParameter!!)
          }
        }
      }
    }
  }

  private fun IrFunction.irBlockBody(bodyBuilderBlock: IrBlockBodyBuilder.() -> Unit) {
    body = DeclarationIrBuilder(pluginContext, this.symbol).irBlockBody(body = bodyBuilderBlock)
  }

  /**
   * Gets the [AutoCloseable.close] of the given property.
   *
   * It is implied that the type of the property implements [AutoCloseable].
   */
  private val IrProperty.closeFunction: IrSimpleFunction
    get() {
      val irClass = backingField!!.type.getClass()!!
      return irClass.functions.single { it.name == closeName }
    }

  /**
   * Checks if the type of the given Property implements [AutoCloseable].
   */
  private fun IrProperty.instanceOfAutoClosable(): Boolean =
    backingField!!.type.superTypes().any { type ->
      type.classFqName == autoCloseableClassId.asSingleFqName()
    }

}
