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
        fun helloWorld() = "Hello, World!"
        fun main() {
          println(helloWorld())
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
