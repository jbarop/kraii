package kraii.ir

import kraii.api.Scoped
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irTry
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName

/**
 * Inserts [AutoCloseable.close] calls for local variables annotated with @Scoped
 * by wrapping subsequent statements in try-finally blocks. This ensures resources
 * are closed on normal exit, early returns, and exceptions.
 */
class KraiiLocalVariableCloseInserter(
  private val pluginContext: IrPluginContext,
) : IrElementTransformerVoid() {

  private val scopedFqName = FqName(Scoped::class.qualifiedName!!)

  override fun visitBody(body: IrBody): IrBody {
    if (body !is IrBlockBody) return super.visitBody(body)
    val result = super.visitBody(body) as IrBlockBody
    wrapScopedVariablesInTryFinally(result.statements)
    return result
  }

  override fun visitBlock(expression: IrBlock): IrBlock {
    val result = super.visitBlock(expression) as IrBlock
    wrapScopedVariablesInTryFinally(result.statements)
    return result
  }

  private fun isScopedVariable(statement: IrStatement): Boolean =
    statement is IrVariable &&
      statement.annotations.any {
        it.type.classFqName == scopedFqName
      }

  private fun wrapScopedVariablesInTryFinally(
    statements: MutableList<IrStatement>,
  ) {
    for (i in statements.indices.reversed()) {
      val stmt = statements[i]
      if (!isScopedVariable(stmt)) continue
      val variable = stmt as IrVariable

      val closeMethod = variable.type.functionByName("close") ?: continue
      val builder = DeclarationIrBuilder(pluginContext, variable.symbol)

      val afterIndex = i + 1
      val statementsAfter = if (afterIndex < statements.size) {
        statements.subList(afterIndex, statements.size).toList().also {
          statements.subList(afterIndex, statements.size).clear()
        }
      } else {
        emptyList()
      }

      val tryBody = builder.irBlock {
        statementsAfter.forEach { +it }
      }

      val finallyBody = builder.irBlock {
        +irCall(closeMethod).apply {
          dispatchReceiver = irGet(variable)
        }
      }

      val tryFinally = builder.irTry(
        type = pluginContext.irBuiltIns.unitType,
        tryResult = tryBody,
        catches = emptyList(),
        finallyExpression = finallyBody,
      )

      statements.add(tryFinally)
    }
  }
}
