package kraii

import kraii.util.compile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class KraiiCheckerTest {

  @Nested
  inner class ScopedMustBeVal {

    @Test
    fun `should reject @Scoped var property`() {
      val result = compile(
        """
        import kraii.api.Scoped

        class Root : AutoCloseable {
          @Scoped
          var resource = object : AutoCloseable {
            override fun close() {}
          }

          override fun close() {}
        }
        """.trimIndent(),
      )

      assertThat(result.success).isFalse()
      assertThat(result.errors).containsExactly(
        "@Scoped must be 'val', not 'var'.",
      )
    }

    @Test
    fun `should reject @Scoped var local variable`() {
      val result = compile(
        """
        import kraii.api.Scoped

        fun testMain() {
          @Scoped var resource = object : AutoCloseable {
            override fun close() {}
          }
        }
        """.trimIndent(),
      )

      assertThat(result.success).isFalse()
      assertThat(result.errors).containsExactly(
        "@Scoped must be 'val', not 'var'.",
      )
    }
  }
}
