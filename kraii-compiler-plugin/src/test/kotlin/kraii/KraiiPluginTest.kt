package kraii

import kraii.util.compileAndRunTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KraiiPluginTest {

  @Test
  fun `should close resources which are annotated with scope`() {
    val result = compileAndRunTest(
      """
        import kraii.api.Scoped
        import kraii.util.CountingResource
        
        class ResourceManager : AutoCloseable {
        
          @Scoped
          private val firstResource = CountingResource()
        
          @Scoped
          private val secondResource = CountingResource()
        }
        
        fun testMain() {
          ResourceManager().close()
        }
      """.trimIndent()
    )

    assertThat(result.numInitialized).isEqualTo(2)
    assertThat(result.numClosed).isEqualTo(2)
  }

  @Test
  fun `should not close resources which are not annotated with scope`() {
    val result = compileAndRunTest(
      """
        import kraii.util.CountingResource
        
        class ResourceManager : AutoCloseable {
        
          private val firstResource = CountingResource()
        
          private val secondResource = CountingResource()
        }
        
        fun testMain() {
          ResourceManager().close()
        }
      """.trimIndent()
    )

    assertThat(result.numInitialized).isEqualTo(2)
    assertThat(result.numClosed).isEqualTo(0)
  }

}
