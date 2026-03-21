package kraii.ir

import kraii.api.Scoped
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irTry
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.impl.IrCatchImpl
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * Adds exception safety to constructors of classes with multiple @Scoped
 * properties.
 *
 * When a class has multiple @Scoped properties, a later property's initializer
 * might throw an exception. Without this transformer, the earlier properties
 * that were already successfully initialized would leak because no close() call
 * would ever run.
 *
 * For each @Scoped property (starting from the second one), this transformer
 * wraps the property initializer in a try-catch that closes all previously
 * initialized @Scoped properties before rethrowing the exception.
 *
 * Example:
 * ```
 * class MyResource : AutoCloseable {
 *   @Scoped val a = ResourceA()
 *   @Scoped val b = ResourceB()
 *   @Scoped val c = ResourceC()
 * }
 * ```
 *
 * This transformer rewrites the initializers to:
 * ```
 * class MyResource : AutoCloseable {
 *   val a = ResourceA()
 *   val b = try {
 *     ResourceB()
 *   } catch (e: Throwable) {
 *     a.close();
 *     throw e
 *   }
 *   val c = try {
 *     ResourceC()
 *   } catch (e: Throwable) {
 *     b.close();
 *     a.close();
 *     throw e
 *   }
 * }
 * ```
 *
 * Note: The first property (`a`) is not wrapped because there are no previous
 * properties to clean up if its initializer throws.
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

    // Exception safety is only needed when there are at least two @Scoped
    // properties. With a single property, there's nothing to clean up if
    // the initializer throws.
    if (scopedProperties.size >= 2) {
      wrapInitializersWithTryCatch(declaration, scopedProperties)
    }

    declaration.acceptChildrenVoid(this)
  }

  /**
   * Wraps each @Scoped property initializer (from the second onward) in a
   * try-catch that closes all previously initialized @Scoped properties on
   * failure.
   */
  private fun wrapInitializersWithTryCatch(
    irClass: IrClass,
    scopedProperties: List<IrProperty>,
  ) {
    val thisReceiver = irClass.thisReceiver
      ?: error("Class ${irClass.name} has no thisReceiver")

    // Start from index 1: the first property has no predecessors to clean up.
    for (i in 1 until scopedProperties.size) {
      val property = scopedProperties[i]
      val backingField = property.backingField
        ?: error("@Scoped property ${property.name} has no backing field")
      val originalInitializer = backingField.initializer?.expression
        ?: error("@Scoped property ${property.name} has no initializer")

      // All @Scoped properties that were initialized before this one.
      // These need to be closed (in reverse order) if this initializer throws.
      val previousProperties = scopedProperties.subList(0, i)

      val builder = DeclarationIrBuilder(pluginContext, backingField.symbol)

      // Build close calls for all previously initialized properties.
      // Reverse order ensures LIFO cleanup (last initialized = first closed).
      val closeStatements = previousProperties
        .reversed()
        .filter { it.type.implements(AutoCloseable::class) }
        // TODO: Iterable<AutoCloseable> properties in catch blocks would need
        // forEach-based cleanup, but that is not yet implemented.
        .map {
          builder.buildCloseAutoCloseable(it, thisReceiver)
        }

      if (closeStatements.isEmpty()) continue

      // try {
      //   <original initializer expression>
      // } catch (e: Throwable) {
      //   prevN.close(); ...; prev1.close()
      //   throw e
      // }
      val catchParameter = buildCatchParameter(irBuiltIns.throwableType)
      val tryCatch = builder.irTry(
        type = backingField.type,
        tryResult = originalInitializer,
        catches = listOf(
          IrCatchImpl(
            startOffset = catchParameter.startOffset,
            endOffset = catchParameter.endOffset,
            catchParameter = catchParameter,
            result = builder.irBlock {
              closeStatements.forEach { +it }
              +buildThrow(
                nothingType = irBuiltIns.nothingType,
                exception = irGet(catchParameter),
              )
            },
          ),
        ),
        finallyExpression = null,
      )

      // Replace the backing field's initializer with the try-catch-wrapped
      // version.
      backingField.initializer = irFactory.createExpressionBody(
        startOffset = tryCatch.startOffset,
        endOffset = tryCatch.endOffset,
        expression = tryCatch,
      )
    }
  }
}
