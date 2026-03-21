package kraii.ir

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrThrowImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.superTypes
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
