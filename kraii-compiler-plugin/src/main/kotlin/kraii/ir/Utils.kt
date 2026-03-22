package kraii.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrThrowImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
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
 * property on a receiver.
 *
 * Generates: `receiver.property.reversed().forEach { it.close() }`
 *
 * @param pluginContext the compiler plugin context for symbol resolution
 * @param property the `Iterable<AutoCloseable>` property to close
 * @param receiver the receiver (`this`) to read the property from
 * @param lambdaParent the parent declaration for the generated lambda
 *   (e.g. the `close()` function or the class containing the property)
 */
fun IrBuilderWithScope.buildCloseIterable(
  pluginContext: IrPluginContext,
  property: IrProperty,
  receiver: IrValueParameter,
  lambdaParent: IrDeclarationParent,
): IrStatement {
  val irFactory = pluginContext.irFactory
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

  // { it: T -> it.close() }
  val lambda = irFactory
    .buildFun {
      name = Name.special("<anonymous>")
      returnType = irBuiltIns.unitType
      origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
      visibility = DescriptorVisibilities.LOCAL
    }.apply {
      val itParam = irFactory
        .createValueParameter(
          startOffset = UNDEFINED_OFFSET,
          endOffset = UNDEFINED_OFFSET,
          origin = IrDeclarationOrigin.DEFINED,
          kind = IrParameterKind.Regular,
          name = Name.identifier("it"),
          type = elementType,
          isAssignable = false,
          symbol = IrValueParameterSymbolImpl(),
          varargElementType = null,
          isCrossinline = false,
          isNoinline = false,
          isHidden = false,
        ).also { it.parent = this }

      parameters = listOf(itParam)

      body = irFactory.createBlockBody(
        startOffset = UNDEFINED_OFFSET,
        endOffset = UNDEFINED_OFFSET,
        statements = listOf(
          irCall(closeMethod).apply {
            dispatchReceiver = irGet(itParam)
          },
        ),
      )
    }

  val functionClass =
    pluginContext
      .finderForBuiltins()
      .findClass(StandardNames.getFunctionClassId(1))
      ?: error("Cannot find Function1 class")
  val functionType = functionClass.typeWith(elementType, irBuiltIns.unitType)

  val reversedSymbol = findIterableExtensionSymbol(pluginContext, "reversed")
  val forEachSymbol = findIterableExtensionSymbol(pluginContext, "forEach")

  // property.reversed().forEach { it.close() }
  return irCall(forEachSymbol).apply {
    // Extension receiver at index 0
    arguments[0] = irCall(reversedSymbol)
      .apply {
        // this.property
        arguments[0] = irCall(
          getter,
          origin = IrStatementOrigin.GET_PROPERTY,
        ).apply {
          dispatchReceiver = irGet(receiver)
        }
      }
    typeArguments[0] = elementType
    arguments[1] = IrFunctionExpressionImpl(
      startOffset = UNDEFINED_OFFSET,
      endOffset = UNDEFINED_OFFSET,
      type = functionType,
      function = lambda,
      origin = IrStatementOrigin.LAMBDA,
    ).also {
      lambda.patchDeclarationParents(lambdaParent)
    }
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
