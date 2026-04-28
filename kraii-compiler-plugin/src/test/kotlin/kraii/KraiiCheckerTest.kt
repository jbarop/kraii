package kraii

import kraii.util.compile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
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

        import kraii.util.NoopResource

        class Root : AutoCloseable {
          @Scoped
          var resource = NoopResource()

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

        import kraii.util.NoopResource

        fun testMain() {
          @Scoped var resource = NoopResource()
        }
        """.trimIndent(),
      )

      assertThat(result.success).isFalse()
      assertThat(result.errors).containsExactly(
        "@Scoped must be 'val', not 'var'.",
      )
    }
  }

  @Nested
  inner class ScopedMustNotAlias {

    @Test
    fun `should reject assigning @Scoped variable to another @Scoped local`() {
      val result = compile(
        """
        import kraii.api.Scoped
        import kraii.util.NoopResource

        fun test() {
          @Scoped val resource = NoopResource()
          @Scoped val alias = resource
        }
        """.trimIndent(),
      )

      assertThat(result.success).isFalse()
      assertThat(result.errors).anyMatch {
        it.contains(
          "@Scoped variable must not be initialized from another @Scoped variable",
        )
      }
    }
  }

  @Nested
  inner class ScopedMustNotEscape {

    @Test
    fun `should reject assigning @Scoped variable to another local`() {
      val result = compile(
        """
        import kraii.api.Scoped
        import kraii.util.NoopResource

        fun test() {
          @Scoped val resource = NoopResource()
          val alias = resource
        }
        """.trimIndent(),
      )

      assertThat(result.success).isFalse()
      assertThat(result.errors).anyMatch {
        it.contains("@Scoped variable must not escape its scope")
      }
    }

    @Test
    fun `should reject returning @Scoped variable`() {
      val result = compile(
        """
        import kraii.api.Scoped
        import kraii.util.NoopResource

        fun createResource(): AutoCloseable {
          @Scoped val resource = NoopResource()
          return resource
        }
        """.trimIndent(),
      )

      assertThat(result.success).isFalse()
      assertThat(result.errors).anyMatch {
        it.contains("@Scoped variable must not escape its scope")
      }
    }

    @Test
    fun `should reject returning @Scoped variable from if branch`() {
      val result = compile(
        """
        import kraii.api.Scoped
        import kraii.util.NoopResource

        fun createResource(flag: Boolean): AutoCloseable? {
          @Scoped val resource = NoopResource()
          if (flag) {
            return resource
          }
          return null
        }
        """.trimIndent(),
      )

      assertThat(result.success).isFalse()
      assertThat(result.errors).anyMatch {
        it.contains("@Scoped variable must not escape its scope")
      }
    }

    @Test
    fun `should reject returning @Scoped variable as expression body`() {
      val result = compile(
        """
        import kraii.api.Scoped
        import kraii.util.NoopResource

        fun createResource(): AutoCloseable = run {
          @Scoped val resource = NoopResource()
          resource
        }
        """.trimIndent(),
      )

      assertThat(result.success).isFalse()
      assertThat(result.errors).anyMatch {
        it.contains("@Scoped variable must not escape its scope")
      }
    }

    @Test
    fun `should reject assigning @Scoped variable to outer var`() {
      val result = compile(
        """
        import kraii.api.Scoped
        import kraii.util.NoopResource

        fun test() {
          var outer: AutoCloseable? = null
          run {
            @Scoped val resource = NoopResource()
            outer = resource
          }
        }
        """.trimIndent(),
      )

      assertThat(result.success).isFalse()
      assertThat(result.errors).anyMatch {
        it.contains("@Scoped variable must not escape its scope")
      }
    }

    @Test
    fun `should reject assigning @Scoped variable to class property`() {
      val result = compile(
        """
        import kraii.api.Scoped
        import kraii.util.NoopResource

        class Holder {
          var held: AutoCloseable? = null

          fun capture() {
            @Scoped val resource = NoopResource()
            held = resource
          }
        }
        """.trimIndent(),
      )

      assertThat(result.success).isFalse()
      assertThat(result.errors).anyMatch {
        it.contains("@Scoped variable must not escape its scope")
      }
    }

    @Test
    fun `should reject passing @Scoped variable as function argument`() {
      val result = compile(
        """
        import kraii.api.Scoped
        import kraii.util.NoopResource

        fun consume(resource: AutoCloseable) {}

        fun test() {
          @Scoped val resource = NoopResource()
          consume(resource)
        }
        """.trimIndent(),
      )

      assertThat(result.success).isFalse()
      assertThat(result.errors).anyMatch {
        it.contains("@Scoped variable must not escape its scope")
      }
    }

    @Test
    fun `should reject passing @Scoped variable as method argument`() {
      val result = compile(
        """
        import kraii.api.Scoped
        import kraii.util.NoopResource

        fun test() {
          val list = mutableListOf<AutoCloseable>()
          @Scoped val resource = NoopResource()
          list.add(resource)
        }
        """.trimIndent(),
      )

      assertThat(result.success).isFalse()
      assertThat(result.errors).anyMatch {
        it.contains("@Scoped variable must not escape its scope")
      }
    }

    @Test
    fun `should reject passing @Scoped variable as constructor argument`() {
      val result = compile(
        """
        import kraii.api.Scoped
        import kraii.util.NoopResource

        fun test() {
          @Scoped val resource = NoopResource()
          val pair = Pair("key", resource)
        }
        """.trimIndent(),
      )

      assertThat(result.success).isFalse()
      assertThat(result.errors).anyMatch {
        it.contains("@Scoped variable must not escape its scope")
      }
    }

    @Disabled("Checker not yet implemented")
    @Test
    fun `should reject capturing @Scoped variable in a lambda`() {
      val result = compile(
        """
        import kraii.api.Scoped
        import kraii.util.NoopResource

        fun test() {
          @Scoped val resource = NoopResource()
          val action: () -> Unit = { resource.close() }
        }
        """.trimIndent(),
      )

      assertThat(result.success).isFalse()
      assertThat(result.errors).anyMatch {
        it.contains("@Scoped variable must not escape its scope")
      }
    }

    @Test
    fun `should reject returning @Scoped variable from lambda`() {
      val result = compile(
        """
        import kraii.api.Scoped
        import kraii.util.NoopResource

        fun test(): AutoCloseable {
          return run {
            @Scoped val resource = NoopResource()
            resource
          }
        }
        """.trimIndent(),
      )

      assertThat(result.success).isFalse()
      assertThat(result.errors).anyMatch {
        it.contains("@Scoped variable must not escape its scope")
      }
    }

    @Test
    fun `should allow calling methods on @Scoped variable`() {
      val result = compile(
        """
        import kraii.api.Scoped

        fun test() {
          @Scoped val resource = object : AutoCloseable {
            override fun close() {}
            fun doWork() {}
          }
          resource.doWork()
        }
        """.trimIndent(),
      )

      assertThat(result.success).isTrue()
    }

    @Test
    fun `should allow accessing properties on @Scoped variable`() {
      val result = compile(
        """
        import kraii.api.Scoped
        import java.io.ByteArrayInputStream

        fun test() {
          @Scoped val stream = ByteArrayInputStream(byteArrayOf(1, 2, 3))
          val available = stream.available()
        }
        """.trimIndent(),
      )

      assertThat(result.success).isTrue()
    }

    @Test
    fun `should allow calling close on @Scoped variable`() {
      val result = compile(
        """
        import kraii.api.Scoped
        import kraii.util.NoopResource

        fun test() {
          @Scoped val resource = NoopResource()
          resource.close()
        }
        """.trimIndent(),
      )

      assertThat(result.success).isTrue()
    }

    @Test
    fun `should allow using result of method call on @Scoped variable`() {
      val result = compile(
        """
        import kraii.api.Scoped

        fun test() {
          @Scoped val resource = object : AutoCloseable {
            override fun close() {}
            override fun toString(): String = "resource"
          }
          println(resource.toString())
        }
        """.trimIndent(),
      )

      assertThat(result.success).isTrue()
    }

    @Test
    fun `should allow @Scoped variable without any usage`() {
      val result = compile(
        """
        import kraii.api.Scoped
        import kraii.util.NoopResource

        fun test() {
          @Scoped val resource = NoopResource()
        }
        """.trimIndent(),
      )

      assertThat(result.success).isTrue()
    }

    @Test
    fun `should allow @Scoped variable as extension receiver`() {
      val result = compile(
        """
        import kraii.api.Scoped
        import java.io.ByteArrayInputStream

        fun ByteArrayInputStream.readAll(): ByteArray = readBytes()

        fun test() {
          @Scoped val stream = ByteArrayInputStream(byteArrayOf(1, 2, 3))
          val bytes = stream.readAll()
        }
        """.trimIndent(),
      )

      assertThat(result.success).isTrue()
    }

    @Test
    fun `should not affect non-scoped local variables`() {
      val result = compile(
        """
        import kraii.util.NoopResource

        fun createResource(): AutoCloseable {
          val resource = NoopResource()
          return resource
        }
        """.trimIndent(),
      )

      assertThat(result.success).isTrue()
    }
  }
}
