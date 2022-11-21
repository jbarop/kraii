package kraii

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.junit.Test
import kotlin.test.assertEquals

class KraiiPluginTest {

  @Test
  fun `should compile`() {
    val source = SourceFile.kotlin(
      name = "main.kt",
      contents = """
        import kotlin.io.path.createTempFile
        import kotlin.io.path.deleteExisting
        
        class ExternalResource : AutoCloseable {
        
          private val tempFile = createTempFile()
        
          override fun close(){
            tempFile.deleteExisting()
          }
        }
        
        class ResourceManager : AutoCloseable {
          private val someThing = "some thing"
          private val firstResource = ExternalResource()
          private val secondResource = ExternalResource()
        }
        
        fun main() {
          ResourceManager().use {
            println("Hello world from Source Code!")
          }
        }
        """.trimIndent()
    )

    val result = KotlinCompilation().apply {
      sources = listOf(source)
      useIR = true
      compilerPlugins = listOf<ComponentRegistrar>(KraiiComponentRegistrar())
      inheritClassPath = true
    }.compile()

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
  }

}
