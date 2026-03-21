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
 * Inserts `close()` calls for local variables annotated with @Scoped by
 * wrapping all subsequent statements in try-finally blocks.
 *
 * This ensures the resource is closed on all exit paths: normal completion,
 * early returns, and exceptions.
 *
 * Example:
 * ```
 * fun work() {
 *   @Scoped val db = Database()
 *   @Scoped val conn = db.connect()
 *   doWork(conn)
 * }
 * ```
 *
 * This transformer rewrites the function body to:
 * ```
 * fun work() {
 *   @Scoped val db = Database()
 *   try {
 *     @Scoped val conn = db.connect()
 *     try {
 *       doWork(conn)
 *     } finally {
 *       conn.close()
 *     }
 *   } finally {
 *     db.close()
 *   }
 * }
 * ```
 *
 * Processing happens in reverse order (last @Scoped variable first) so that
 * the nesting is correct: the innermost try-finally wraps only the statements
 * after the last @Scoped variable, and each outer try-finally wraps its inner
 * one.
 *
 * Note: Unlike the close() method generator, this does NOT use exception
 * aggregation (addSuppressed). A finally-block exception replaces the original.
 * This matches Kotlin's standard try-finally semantics.
 */
class KraiiLocalVariableCloseInserter(
  private val pluginContext: IrPluginContext,
) : IrElementTransformerVoid() {

  private val scopedFqName = FqName(Scoped::class.qualifiedName!!)

  /**
   * Transform function bodies (`IrBlockBody`).
   * First recurse into children (so nested blocks are handled), then
   * wrap @Scoped variables at this level.
   */
  override fun visitBody(body: IrBody): IrBody {
    if (body !is IrBlockBody) return super.visitBody(body)
    val result = super.visitBody(body) as IrBlockBody
    wrapScopedVariablesInTryFinally(result.statements)
    return result
  }

  /**
   * Transform block expressions (e.g., if/when branches, lambdas).
   * Same strategy: recurse first, then wrap at this level.
   */
  override fun visitBlock(expression: IrBlock): IrBlock {
    val result = super.visitBlock(expression) as IrBlock
    wrapScopedVariablesInTryFinally(result.statements)
    return result
  }

  /** Checks whether a statement is a local variable annotated with @Scoped. */
  private fun isScopedVariable(statement: IrStatement): Boolean =
    statement is IrVariable &&
      statement.annotations.any {
        it.type.classFqName == scopedFqName
      }

  /**
   * Finds all @Scoped local variables in the statement list and wraps
   * everything after each one in a try-finally block.
   *
   * Iterates in reverse so that inner wrappings happen first (creating
   * correct nesting when multiple @Scoped variables exist in sequence).
   *
   * Before : `[stmt0, @Scoped val a = ..., stmt1, stmt2]`
   * After  : `[stmt0, try { stmt1; stmt2 } finally { a.close() }]`
   *
   * The @Scoped variable declaration itself is removed from the statement list
   * and becomes part of the outer scope (it's the variable referenced in
   * the finally block, so it must remain visible).
   *
   * Wait — actually the variable stays in the list. Only the statements *after*
   * it get moved into the try body. The final statement list becomes:
   * `[stmt0, @Scoped val a = ..., try { stmt1; stmt2 } finally { a.close() }]`
   */
  private fun wrapScopedVariablesInTryFinally(
    statements: MutableList<IrStatement>,
  ) {
    for (i in statements.indices.reversed()) {
      val stmt = statements[i]
      if (!isScopedVariable(stmt)) continue
      val variable = stmt as IrVariable

      val closeMethod = variable.type.functionByName("close")
        ?: error(
          "close() not found on type ${variable.type}" +
            " for @Scoped variable ${variable.name}",
        )
      val builder = DeclarationIrBuilder(pluginContext, variable.symbol)

      // Extract all statements after the @Scoped variable declaration.
      // These are the statements that need to run inside the try block.
      val statementsAfter = extractStatementsFrom(statements, fromIndex = i + 1)

      // try {
      //   <statements after @Scoped variable>
      // } finally {
      //   variable.close()
      // }
      //
      // The statement list becomes:
      // [..., @Scoped val x = ..., try { ... } finally { x.close() }]
      statements.add(
        builder.irTry(
          type = pluginContext.irBuiltIns.unitType,
          tryResult = builder.irBlock {
            statementsAfter.forEach { +it }
          },
          catches = emptyList(),
          finallyExpression = builder.irBlock {
            // variable.close()
            +irCall(closeMethod).apply {
              dispatchReceiver = irGet(variable)
            }
          },
        ),
      )
    }
  }

  /**
   * Removes and returns all statements from [fromIndex] to the end of the list.
   * Returns an empty list if [fromIndex] is past the end.
   */
  private fun extractStatementsFrom(
    statements: MutableList<IrStatement>,
    fromIndex: Int,
  ): List<IrStatement> {
    if (fromIndex >= statements.size) return emptyList()
    val extracted = statements.subList(fromIndex, statements.size).toList()
    statements.subList(fromIndex, statements.size).clear()
    return extracted
  }
}
