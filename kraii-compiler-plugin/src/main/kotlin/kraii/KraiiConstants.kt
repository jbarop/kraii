package kraii

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object KraiiPluginKey : GeneratedDeclarationKey() {
  override fun toString() = "kraii-compiler-plugin"
}

val autoCloseableClassId = ClassId(
  FqName("java.lang"),
  Name.identifier("AutoCloseable"),
)

val closeName = Name.identifier("close")
