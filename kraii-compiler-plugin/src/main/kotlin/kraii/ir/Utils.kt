package kraii.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.createTmpVariable
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irEqualsNull
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irIfThenElse
import org.jetbrains.kotlin.ir.builders.irNotEquals
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irTry
import org.jetbrains.kotlin.ir.builders.irWhile
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCatchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrThrowImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import kotlin.reflect.KClass

/**
 * Checks if the property is annotated with the specified annotation.
 */
fun IrProperty.isAnnotatedWith(kClass: KClass<*>): Boolean =
  annotations.any { annotationCall ->
    annotationCall.type.classFqName == FqName(kClass.qualifiedName!!)
  }

/**
 * The type of the property, obtained from its backing field.
 * Only field-backed properties are supported.
 */
val IrProperty.type: IrSimpleType
  get() = (
    backingField?.type ?: error(
      "Only field properties are supported.",
    )
  ) as IrSimpleType

/**
 * Checks if the type implements the given class by walking the supertype hierarchy.
 */
fun IrType.implements(kClass: KClass<*>): Boolean =
  superTypes().any {
    it.classFqName == FqName(kClass.qualifiedName!!) ||
      it.implements(kClass)
  }

/**
 * Finds a no-arg function by name on the type's class declaration.
 * Returns null if the type has no class or no matching function.
 */
fun IrType.functionByName(name: String): IrSimpleFunction? =
  getClass()?.functions?.single { it.name == Name.identifier(name) }

/**
 * Builds IR for reading a property via its getter on a receiver.
 *
 * Generates: `receiver.property`
 */
fun IrBuilderWithScope.buildGetProperty(
  property: IrProperty,
  receiver: IrValueParameter,
): IrExpression {
  val getter = property.getter
    ?: error("Property ${property.name} has no getter")
  return irCall(getter, origin = IrStatementOrigin.GET_PROPERTY).apply {
    dispatchReceiver = irGet(receiver)
  }
}

/**
 * Builds IR for closing an `AutoCloseable` property on a receiver.
 *
 * Generates: `receiver.property.close()`
 */
fun IrBuilderWithScope.buildCloseAutoCloseable(
  property: IrProperty,
  receiver: IrValueParameter,
): IrStatement {
  val getProperty = buildGetProperty(property, receiver)
  val closeMethod = property.type.functionByName("close")
    ?: error(
      "close() not found on ${property.type}" +
        " for property ${property.name}",
    )
  return irCall(closeMethod).apply {
    dispatchReceiver = getProperty
  }
}

/**
 * Builds IR for closing all elements in an `Iterable<AutoCloseable>`
 * property on a receiver, with per-element exception safety.
 *
 * Each element's `close()` is wrapped in a try-catch so that a failing
 * close doesn't prevent remaining elements from being closed. Exceptions
 * are aggregated via `addSuppressed()`.
 *
 * Generates:
 * ```
 * val $reversed = receiver.property.reversed()
 * var $closeException: Throwable? = null
 * val $iterator = $reversed.iterator()
 * while ($iterator.hasNext()) {
 *     val $element = $iterator.next()
 *     try {
 *         $element.close()
 *     } catch (e: Throwable) {
 *         if ($closeException == null) $closeException = e
 *         else $closeException.addSuppressed(e)
 *     }
 * }
 * if ($closeException != null) throw $closeException
 * ```
 *
 * @param pluginContext the compiler plugin context for symbol resolution
 * @param property the `Iterable<AutoCloseable>` property to close
 * @param receiver the receiver (`this`) to read the property from
 */
fun IrBuilderWithScope.buildCloseIterable(
  pluginContext: IrPluginContext,
  property: IrProperty,
  receiver: IrValueParameter,
): IrStatement {
  val irBuiltIns = pluginContext.irBuiltIns

  val getter = property.getter
    ?: error("Property ${property.name} has no getter")
  val elementType = property.type
    .arguments
    .first()
    .typeOrNull
    ?: error(
      "Cannot resolve type argument for Iterable property ${property.name}",
    )

  val closeMethod = elementType.functionByName("close")
    ?: error("close() method not found on $elementType")

  val reversedSymbol = findIterableExtensionSymbol(pluginContext, "reversed")

  val iteratorFn = irBuiltIns.iterableClass.owner.functions.single {
    it.name == OperatorNameConventions.ITERATOR
  }
  val hasNextFn = irBuiltIns.iteratorClass.owner.functions.single {
    it.name == OperatorNameConventions.HAS_NEXT
  }
  val nextFn = irBuiltIns.iteratorClass.owner.functions.single {
    it.name == OperatorNameConventions.NEXT
  }

  val addSuppressedMethod = irBuiltIns.throwableType
    .getClass()
    ?.functions
    ?.firstOrNull { it.name == Name.identifier("addSuppressed") }
    ?: error(
      "Throwable.addSuppressed() not found. " +
        "kraii requires a JVM target that provides this method.",
    )

  return irBlock {
    // val $reversed = receiver.property.reversed()
    val reversedVar = createTmpVariable(
      irExpression = irCall(reversedSymbol).apply {
        arguments[0] = irCall(
          getter,
          origin = IrStatementOrigin.GET_PROPERTY,
        ).apply {
          dispatchReceiver = irGet(receiver)
        }
      },
      nameHint = $$"$reversed",
    )

    // var $closeException: Throwable? = null
    val declarationParent = scope.getLocalDeclarationParent()
    val exceptionVar = IrVariableImpl(
      startOffset = UNDEFINED_OFFSET,
      endOffset = UNDEFINED_OFFSET,
      origin = IrDeclarationOrigin.DEFINED,
      symbol = IrVariableSymbolImpl(),
      name = Name.identifier($$"$closeException"),
      type = irBuiltIns.throwableType.makeNullable(),
      isVar = true,
      isConst = false,
      isLateinit = false,
    ).also {
      it.parent = declarationParent
      it.initializer = irNull()
    }
    +exceptionVar

    // val $iterator = $reversed.iterator()
    val iteratorVar = createTmpVariable(
      irExpression = irCall(iteratorFn).apply {
        dispatchReceiver = irGet(reversedVar)
      },
      nameHint = $$"$iterator",
    )

    // while ($iterator.hasNext()) { ... }
    +irWhile().apply {
      // $iterator.hasNext()
      condition = irCall(hasNextFn).apply {
        dispatchReceiver = irGet(iteratorVar)
      }
      body = irBlock {
        // val $element = $iterator.next()
        val elementVar = createTmpVariable(
          irExpression = irCall(nextFn).apply {
            dispatchReceiver = irGet(iteratorVar)
          },
          nameHint = $$"$element",
          irType = elementType,
        )

        val catchParam = buildCatchParameter(irBuiltIns.throwableType).also {
          it.parent = declarationParent
        }

        // try { $element.close() }
        // catch (e: Throwable) {
        //   if ($closeException == null) $closeException = e
        //   else $closeException.addSuppressed(e)
        // }
        +irTry(
          type = irBuiltIns.unitType,
          tryResult = irCall(closeMethod).apply {
            dispatchReceiver = irGet(elementVar)
          },
          catches = listOf(
            IrCatchImpl(
              startOffset = UNDEFINED_OFFSET,
              endOffset = UNDEFINED_OFFSET,
              catchParameter = catchParam,
              result = irIfThenElse(
                type = irBuiltIns.unitType,
                condition = irEqualsNull(irGet(exceptionVar)),
                thenPart = irSet(exceptionVar, irGet(catchParam)),
                elsePart = irCall(addSuppressedMethod).apply {
                  dispatchReceiver = irGet(exceptionVar)
                  arguments[1] = irGet(catchParam)
                },
              ),
            ),
          ),
          finallyExpression = null,
        )
      }
    }

    // if ($closeException != null) throw $closeException
    +irIfThen(
      type = irBuiltIns.unitType,
      condition = irNotEquals(irGet(exceptionVar), irNull()),
      thenPart = buildThrow(
        nothingType = irBuiltIns.nothingType,
        exception = irGet(exceptionVar),
      ),
    )
  }
}

/**
 * Finds an extension function on `Iterable` from `kotlin.collections` by
 * name (e.g. `"forEach"`, `"reversed"`).
 */
private fun findIterableExtensionSymbol(
  pluginContext: IrPluginContext,
  functionName: String,
) = pluginContext
  .finderForBuiltins()
  .findFunctions(
    CallableId(FqName("kotlin.collections"), Name.identifier(functionName)),
  ).single { symbol ->
    symbol.owner.parameters.any { param ->
      param.kind == IrParameterKind.ExtensionReceiver &&
        param.type.getClass() == pluginContext.irBuiltIns.iterableClass.owner
    }
  }

/**
 * Creates an IR variable for use as a catch parameter.
 *
 * Generates the parameter part of: `catch (e: Throwable)`
 */
fun buildCatchParameter(
  throwableType: IrType,
  name: String = "e",
): IrVariableImpl =
  IrVariableImpl(
    startOffset = UNDEFINED_OFFSET,
    endOffset = UNDEFINED_OFFSET,
    origin = IrDeclarationOrigin.CATCH_PARAMETER,
    symbol = IrVariableSymbolImpl(),
    name = Name.identifier(name),
    type = throwableType,
    isVar = false,
    isConst = false,
    isLateinit = false,
  )

/**
 * Creates an IR throw expression.
 *
 * Generates: `throw expression`
 */
fun buildThrow(
  nothingType: IrType,
  exception: IrExpression,
): IrThrowImpl =
  IrThrowImpl(
    startOffset = UNDEFINED_OFFSET,
    endOffset = UNDEFINED_OFFSET,
    type = nothingType,
    value = exception,
  )
