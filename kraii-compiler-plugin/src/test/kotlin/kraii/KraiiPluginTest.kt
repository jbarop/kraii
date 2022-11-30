package kraii

import kraii.util.compileAndRunTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KraiiPluginTest {

  @Test
  fun `should compile`() {
    val result = compileAndRunTest(
      """
        import kraii.api.Scoped
        import kraii.util.CountingResource
        
        class ResourceManager : AutoCloseable {
        
          @Scoped
          private val firstResource = CountingResource()
        
          @Scoped
          private val secondResource = CountingResource()
        
          private val unscopedResource = CountingResource()
        }
        
        fun testMain() {
          ResourceManager().use {
            println("Hello world from Source Code!")
          }
        }
      """.trimIndent()
    )

    assertThat(result.numInitialized).isEqualTo(3)
    assertThat(result.numClosed).isEqualTo(2)
  }

}
