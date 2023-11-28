package kraii.ir

import kraii.api.Scoped
import kraii.closeName
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.GET_PROPERTY
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name

/**
 * Generates the implementation of [AutoCloseable.close].
 */
class KraiiClassLoweringPass(
  private val pluginContext: IrPluginContext,
) : ClassLoweringPass {

  override fun lower(irClass: IrClass) {
    if (!irClass.defaultType.implements(AutoCloseable::class)) return
    val thisCloseFunction = irClass.functions.single { it.name == closeName }
    val existingStatements = thisCloseFunction.body?.statements ?: emptyList()
    val propertiesToClose = irClass.properties
      .filter { it.isAnnotatedWith(Scoped::class) }
      .toList().reversed()

    thisCloseFunction.irBlockBody {
      existingStatements.forEach { statement -> +statement }
      propertiesToClose.forEach { propertyToClose ->
        if (propertyToClose.type.implements(AutoCloseable::class)) {
          val closeFunction = propertyToClose.type.functionByName("close")
            ?: error("`close()` function not found")
          +irCall(closeFunction).apply {
            dispatchReceiver = irCall(propertyToClose.getter!!, origin = GET_PROPERTY).apply {
              dispatchReceiver = irGet(thisCloseFunction.dispatchReceiverParameter!!)
            }
          }
        } else if (propertyToClose.type.implements(Iterable::class)) {
          val builder: IrBuilderWithScope =
            DeclarationIrBuilder(pluginContext, thisCloseFunction.symbol)
          +builder.buildStatement(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
          ) {
            irCall(pluginContext.iterableForEach).apply {
              extensionReceiver = irCall(pluginContext.iterableReversed).apply {
                extensionReceiver = irCall(propertyToClose.getter!!, GET_PROPERTY).apply {
                  dispatchReceiver = irGet(thisCloseFunction.dispatchReceiverParameter!!)
                }
              }

              val elementType = propertyToClose.type.arguments.first().typeOrNull!!
              val lambda = pluginContext.irFactory.buildFun {
                name = Name.special("<anonymous>")
                returnType = pluginContext.irBuiltIns.unitType
                origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
                visibility = DescriptorVisibilities.LOCAL
              }.apply {
                addValueParameter("it", elementType)
                body = DeclarationIrBuilder(pluginContext, this.symbol).irBlockBody {
                  val elementToClose = valueParameters.first()
                  val closeFunction = elementType.functionByName("close")!!

                  +irCall(closeFunction).apply {
                    dispatchReceiver = irGet(elementToClose)
                  }
                }
              }

              val lambdaClass = pluginContext.referenceClass(
                StandardNames.getFunctionClassId(lambda.allParameters.size)
              ) ?: error("Cannot find function base class for lambda!")
              val lambdaType = lambdaClass.typeWith(elementType, lambda.returnType)

              putTypeArgument(0, propertyToClose.type.arguments.first().typeOrNull)
              putValueArgument(
                0, IrFunctionExpressionImpl(
                  startOffset = UNDEFINED_OFFSET,
                  endOffset = UNDEFINED_OFFSET,
                  type = lambdaType,
                  function = lambda,
                  origin = IrStatementOrigin.LAMBDA,
                ).apply {
                  lambda.patchDeclarationParents(parent)
                }
              )
            }
          }
        }
      }
    }
  }

  private fun IrFunction.irBlockBody(bodyBuilderBlock: IrBlockBodyBuilder.() -> Unit) {
    body = DeclarationIrBuilder(pluginContext, this.symbol).irBlockBody(body = bodyBuilderBlock)
  }

}
