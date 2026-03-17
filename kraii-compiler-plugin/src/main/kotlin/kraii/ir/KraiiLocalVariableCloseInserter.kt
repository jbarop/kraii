package kraii.ir

import kraii.api.Scoped
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName

/**
 * Inserts [AutoCloseable.close] calls for local variables annotated with @Scoped
 * at the end of the enclosing block body and before return statements.
 */
class KraiiLocalVariableCloseInserter(
  private val pluginContext: IrPluginContext,
) : IrElementTransformerVoid() {

  private val scopedFqName = FqName(Scoped::class.qualifiedName!!)
  private val scopeStack = mutableListOf<MutableList<IrVariable>>()

  override fun visitBody(body: IrBody): IrBody {
    if (body !is IrBlockBody) return super.visitBody(body)
    scopeStack.add(mutableListOf())
    val result = super.visitBody(body) as IrBlockBody
    val currentScope = scopeStack.removeLast()
    appendCloseCallsForVariables(result.statements, currentScope)
    return result
  }

  override fun visitBlock(expression: IrBlock): IrBlock {
    scopeStack.add(mutableListOf())
    val result = super.visitBlock(expression) as IrBlock
    val currentScope = scopeStack.removeLast()
    appendCloseCallsForVariables(result.statements, currentScope)
    return result
  }

  override fun visitVariable(declaration: IrVariable): IrStatement {
    val result = super.visitVariable(declaration)
    if (result is IrVariable &&
      result.annotations.any {
        it.type.classFqName == scopedFqName
      }
    ) {
      scopeStack.lastOrNull()?.add(result)
    }
    return result
  }

  override fun visitReturn(expression: IrReturn): IrExpression {
    val result = super.visitReturn(expression) as IrReturn

    val allScoped = scopeStack.flatten()
    if (allScoped.isEmpty()) return result

    val builder = DeclarationIrBuilder(pluginContext, result.returnTargetSymbol)

    return builder.irBlock {
      val tmp = irTemporary(result.value)
      for (variable in allScoped.reversed()) {
        val closeMethod = variable.type.functionByName("close") ?: continue
        +irCall(closeMethod).apply {
          dispatchReceiver = irGet(variable)
        }
      }
      +result.apply { value = irGet(tmp) }
    }
  }

  private fun appendCloseCallsForVariables(
    statements: MutableList<IrStatement>,
    scopedVariables: List<IrVariable>,
  ) {
    if (scopedVariables.isEmpty()) return

    val closeCalls = mutableListOf<IrStatement>()

    for (variable in scopedVariables.reversed()) {
      val closeMethod = variable.type.functionByName("close") ?: continue
      val builder = DeclarationIrBuilder(pluginContext, variable.symbol)
      val callClose = builder.irCall(closeMethod).apply {
        dispatchReceiver = builder.irGet(variable)
      }
      closeCalls.add(callClose)
    }

    statements.addAll(closeCalls)
  }
}
