package kraii.ir

import kraii.api.Scoped
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irTry
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCatchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrThrowImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name

/**
 * Adds exception safety to constructors of classes with multiple @Scoped properties.
 *
 * When a later property's initializer throws, all previously initialized @Scoped
 * properties are closed before the exception propagates.
 */
class KraiiConstructorExceptionSafetyTransformer(
  private val pluginContext: IrPluginContext,
) : IrVisitorVoid() {

  private val irFactory = pluginContext.irFactory
  private val irBuiltIns = pluginContext.irBuiltIns

  override fun visitElement(element: IrElement) {
    when (element) {
      is IrDeclaration,
      is IrFile,
      is IrModuleFragment,
      -> element.acceptChildrenVoid(this)

      else -> {}
    }
  }

  override fun visitClass(declaration: IrClass) {
    val scopedProperties = declaration.properties
      .filter { it.isAnnotatedWith(Scoped::class) }
      .filter { it.backingField?.initializer != null }
      .toList()

    if (scopedProperties.size >= 2) {
      wrapInitializersWithTryCatch(declaration, scopedProperties)
    }

    declaration.acceptChildrenVoid(this)
  }

  private fun wrapInitializersWithTryCatch(
    irClass: IrClass,
    scopedProperties: List<IrProperty>,
  ) {
    val thisReceiver = irClass.thisReceiver ?: return

    for (i in 1 until scopedProperties.size) {
      val property = scopedProperties[i]
      val backingField = property.backingField ?: continue
      val originalExpr = backingField.initializer?.expression ?: continue
      val previousProperties = scopedProperties.subList(0, i)

      val builder = DeclarationIrBuilder(pluginContext, backingField.symbol)

      val exceptionVar = IrVariableImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        IrDeclarationOrigin.CATCH_PARAMETER,
        IrVariableSymbolImpl(),
        Name.identifier("e"),
        irBuiltIns.throwableType,
        isVar = false,
        isConst = false,
        isLateinit = false,
      )

      val closeStatements = mutableListOf<IrStatement>()
      for (prev in previousProperties.reversed()) {
        val propertyType = prev.type
        if (propertyType.implements(AutoCloseable::class)) {
          val stmt = builder.buildCloseAutoCloseable(prev, thisReceiver)
          if (stmt != null) closeStatements.add(stmt)
        }
        // Iterable<AutoCloseable> properties in catch blocks would need
        // forEach-based cleanup, but that is not yet implemented.
      }

      if (closeStatements.isEmpty()) continue

      val catchBody = builder.irBlock {
        closeStatements.forEach { +it }
        +IrThrowImpl(
          UNDEFINED_OFFSET,
          UNDEFINED_OFFSET,
          irBuiltIns.nothingType,
          irGet(exceptionVar),
        )
      }

      val catch = IrCatchImpl(
        startOffset = UNDEFINED_OFFSET,
        endOffset = UNDEFINED_OFFSET,
        catchParameter = exceptionVar,
        result = catchBody,
      )

      val tryCatch = builder.irTry(
        type = backingField.type,
        tryResult = originalExpr,
        catches = listOf(catch),
        finallyExpression = null,
      )

      backingField.initializer = irFactory.createExpressionBody(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        tryCatch,
      )
    }
  }

  private fun IrBuilderWithScope.buildCloseAutoCloseable(
    property: IrProperty,
    thisReceiver: IrValueParameter,
  ): IrStatement? {
    val getter = property.getter ?: return null
    val closeMethod = property.type.functionByName("close") ?: return null

    val getProperty = irCall(
      getter,
      origin = IrStatementOrigin.GET_PROPERTY,
    ).apply {
      dispatchReceiver = irGet(thisReceiver)
    }

    return irCall(closeMethod).apply {
      dispatchReceiver = getProperty
    }
  }
}
