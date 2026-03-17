package kraii.ir

import kraii.api.Scoped
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName

/**
 * Inserts [AutoCloseable.close] calls for local variables annotated with @Scoped
 * at the end of the enclosing block body.
 */
class KraiiLocalVariableCloseInserter(
  private val pluginContext: IrPluginContext,
) : IrElementTransformerVoid() {

  private val scopedFqName = FqName(Scoped::class.qualifiedName!!)

  override fun visitBody(body: IrBody): IrBody {
    val result = super.visitBody(body)
    if (result !is IrBlockBody) return result

    val scopedVariables = result.statements
      .filterIsInstance<IrVariable>()
      .filter { variable ->
        variable.annotations.any { it.type.classFqName == scopedFqName }
      }

    if (scopedVariables.isEmpty()) return result

    val closeCalls = mutableListOf<IrStatement>()

    for (variable in scopedVariables.reversed()) {
      val closeMethod = variable.type.functionByName("close") ?: continue
      val builder = DeclarationIrBuilder(pluginContext, variable.symbol)
      val callClose = builder.irCall(closeMethod).apply {
        dispatchReceiver = builder.irGet(variable)
      }
      closeCalls.add(callClose)
    }

    result.statements.addAll(closeCalls)

    return result
  }
}
