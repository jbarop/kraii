package kraii

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces

val scopedClassId = ClassId(
  FqName("kraii.api"),
  Name.identifier("Scoped")
)

/**
 * Full class name of [AutoCloseable].
 */
val autoCloseableClassId = ClassId(
  FqName("java.lang"),
  Name.identifier("AutoCloseable")
)

/**
 * The [Name] of the [AutoCloseable.close] method.
 */
val closeName = Name.identifier("close")

/**
 * Checks if a class implements [AutoCloseable].
 */
fun ClassDescriptor.implementsAutoClosable(): Boolean =
  getSuperInterfaces().any {
    (it.fqNameOrNull() == autoCloseableClassId.asSingleFqName()) || it.implementsAutoClosable()
  }
