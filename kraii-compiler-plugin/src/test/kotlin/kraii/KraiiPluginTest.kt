package kraii

import kraii.util.compileAndRunTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KraiiPluginTest {

  @Test
  fun `should close resources which are annotated with @Scoped`() {
    val result = compileAndRunTest(
      """
        import kraii.api.Scoped
        import kraii.util.CountingResource
        
        class Root : AutoCloseable {
          @Scoped
          private val resource = CountingResource("resource")
        }
        
        fun testMain() {
          Root().close()
        }
      """.trimIndent()
    )

    assertThat(result.initialized).isEqualTo(listOf("resource"))
    assertThat(result.closed).isEqualTo(listOf("resource"))
  }

  @Test
  fun `should not close resources which are not annotated with @Scoped`() {
    val result = compileAndRunTest(
      """
        import kraii.util.CountingResource
        
        class Root : AutoCloseable {
          private val resource = CountingResource("resource")
        }
        
        fun testMain() {
          Root().close()
        }
      """.trimIndent()
    )

    assertThat(result.initialized).isEqualTo(listOf("resource"))
    assertThat(result.closed).isEqualTo(emptyList<String>())
  }

  @Test
  fun `should close resources in reverse order than the declaration`() {
    val result = compileAndRunTest(
      """
        import kraii.api.Scoped
        import kraii.util.CountingResource
        
        class Root : AutoCloseable {
          @Scoped
          private val resource1 = CountingResource("resource1-1")
          @Scoped
          private val resource2 = Child()
          @Scoped
          private val resource3 = CountingResource("resource1-3")
        }
        
        class Child : AutoCloseable {
          @Scoped
          private val resource1 = CountingResource("resource2-1")
          @Scoped
          private val resource2 = CountingResource("resource2-2")
        }
        
        fun testMain() {
          Root().close()
        }
      """.trimIndent()
    )

    assertThat(result.initialized).isEqualTo(
      listOf(
        "resource1-1",
        "resource2-1",
        "resource2-2",
        "resource1-3",
      )
    )
    assertThat(result.closed).isEqualTo(
      listOf(
        "resource1-3",
        "resource2-2",
        "resource2-1",
        "resource1-1",
      )
    )
  }

}
