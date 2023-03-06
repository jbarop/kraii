package kraii

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import kotlin.reflect.KClass

/**
 * The [Name] of the [AutoCloseable.close] method.
 */
val closeName = Name.identifier("close")

/**
 * Checks if a class implements [AutoCloseable].
 */
fun ClassDescriptor.implementsAutoClosable(): Boolean =
  getSuperInterfaces().any {
    (it.fqNameOrNull() == FqName(AutoCloseable::class.qualifiedName!!)) || it.implementsAutoClosable()
  }

/**
 * Checks if the property is annotated with the specified annotation.
 */
fun IrProperty.isAnnotatedWith(kClass: KClass<*>): Boolean =
  annotations.any { annotationCall ->
    annotationCall.type.classFqName == FqName(kClass.qualifiedName!!)
  }

/**
 * The type of the property.
 */
val IrProperty.type: IrSimpleType
  get() = (backingField?.type ?: error("Only field properties are supported.")) as IrSimpleType

/**
 * Checks if the type implements the given class.
 */
fun IrType.implements(kClass: KClass<*>): Boolean =
  superTypes().any { it.classFqName == FqName(kClass.qualifiedName!!) || it.implements(kClass) }

/**
 * Finds the function by name.
 */
fun IrType.functionByName(name: String): IrSimpleFunction? =
  getClass()?.functions?.single { it.name == Name.identifier(name) }

/**
 * Finds the extension function [Iterable<T>.forEach].
 */
val IrPluginContext.iterableForEach: IrSimpleFunctionSymbol
  get() =
    referenceFunctions(
      CallableId(
        FqName("kotlin.collections"),
        Name.identifier("forEach"),
      )
    ).single {
      it.owner.extensionReceiverParameter!!.type.classFqName == irBuiltIns.iterableClass.owner.kotlinFqName
    }

/**
 * Finds the extension function [Iterable<T>.reversed].
 */
val IrPluginContext.iterableReversed: IrSimpleFunctionSymbol
  get() =
    referenceFunctions(
      CallableId(
        FqName("kotlin.collections"),
        Name.identifier("reversed"),
      )
    ).single {
      it.owner.extensionReceiverParameter!!.type.classFqName == irBuiltIns.iterableClass.owner.kotlinFqName
    }
