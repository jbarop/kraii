package kraii

import kraii.util.compileAndRunTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class KraiiPropertyTest {

  @Nested
  inner class BasicCleanup {

    @Test
    fun `should close single scoped property`() {
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
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("resource"))
      assertThat(result.closed).isEqualTo(listOf("resource"))
    }

    @Test
    fun `should close multiple scoped properties in reverse order`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        class Root : AutoCloseable {
          @Scoped
          private val a = CountingResource("a")
          @Scoped
          private val b = CountingResource("b")
          @Scoped
          private val c = CountingResource("c")
        }

        fun testMain() {
          Root().close()
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("a", "b", "c"))
      assertThat(result.closed).isEqualTo(listOf("c", "b", "a"))
    }

    @Test
    fun `should not close non-scoped properties`() {
      val result = compileAndRunTest(
        """
        import kraii.util.CountingResource

        class Root : AutoCloseable {
          private val resource = CountingResource("resource")
        }

        fun testMain() {
          Root().close()
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("resource"))
      assertThat(result.closed).isEqualTo(emptyList<String>())
    }

    @Test
    fun `should close only scoped properties when mixed with non-scoped`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        class Root : AutoCloseable {
          private val notScoped1 = CountingResource("notScoped1")
          @Scoped
          private val scoped1 = CountingResource("scoped1")
          private val notScoped2 = CountingResource("notScoped2")
          @Scoped
          private val scoped2 = CountingResource("scoped2")
        }

        fun testMain() {
          Root().close()
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(
        listOf("notScoped1", "scoped1", "notScoped2", "scoped2"),
      )
      assertThat(result.closed).isEqualTo(listOf("scoped2", "scoped1"))
    }
  }

  @Nested
  inner class Containers {

    @Test
    fun `should close resources in scoped Iterable container`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        class Root : AutoCloseable {
          @Scoped
          private val container = listOf(
            CountingResource("resource"),
          )
        }

        fun testMain() {
          Root().close()
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("resource"))
      assertThat(result.closed).isEqualTo(listOf("resource"))
    }

    @Test
    fun `should not close resources in non-scoped container`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        class Root : AutoCloseable {
          private val container = listOf(
            CountingResource("resource"),
          )
        }

        fun testMain() {
          Root().close()
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("resource"))
      assertThat(result.closed).isEqualTo(emptyList<String>())
    }

    @Test
    fun `should close resources in container in reverse element order`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        class Root : AutoCloseable {
          @Scoped
          private val container = listOf(
            CountingResource("a"),
            CountingResource("b"),
            CountingResource("c"),
          )
        }

        fun testMain() {
          Root().close()
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("a", "b", "c"))
      assertThat(result.closed).isEqualTo(listOf("c", "b", "a"))
    }

    @Test
    fun `should close multiple containers in reverse declaration order`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        class Root : AutoCloseable {
          @Scoped
          private val container1 = listOf(
            CountingResource("c1-a"),
            CountingResource("c1-b"),
          )
          @Scoped
          private val container2 = listOf(
            CountingResource("c2-a"),
            CountingResource("c2-b"),
          )
        }

        fun testMain() {
          Root().close()
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(
        listOf("c1-a", "c1-b", "c2-a", "c2-b"),
      )
      assertThat(result.closed).isEqualTo(
        listOf("c2-b", "c2-a", "c1-b", "c1-a"),
      )
    }

    @Test
    fun `should handle mixed direct properties and containers`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        class Root : AutoCloseable {
          @Scoped
          private val direct = CountingResource("direct")
          @Scoped
          private val container = listOf(
            CountingResource("in-container-a"),
            CountingResource("in-container-b"),
          )
        }

        fun testMain() {
          Root().close()
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(
        listOf("direct", "in-container-a", "in-container-b"),
      )
      assertThat(result.closed).isEqualTo(
        listOf("in-container-b", "in-container-a", "direct"),
      )
    }
  }

  @Nested
  inner class NestedClasses {

    @Test
    fun `should close nested AutoCloseable hierarchy in reverse order`() {
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
          private val container2 = listOf(
            CountingResource("resource2-2-1"),
            CountingResource("resource2-2-2"),
            CountingResource("resource2-2-3"),
          )
          @Scoped
          private val resource3 = CountingResource("resource2-3")
        }

        fun testMain() {
          Root().close()
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(
        listOf(
          "resource1-1",
          "resource2-1",
          "resource2-2-1",
          "resource2-2-2",
          "resource2-2-3",
          "resource2-3",
          "resource1-3",
        ),
      )
      assertThat(result.closed).isEqualTo(
        listOf(
          "resource1-3",
          "resource2-3",
          "resource2-2-3",
          "resource2-2-2",
          "resource2-2-1",
          "resource2-1",
          "resource1-1",
        ),
      )
    }

    @Test
    fun `should close deeply nested hierarchy`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        class Level3 : AutoCloseable {
          @Scoped
          private val r = CountingResource("level3")
        }

        class Level2 : AutoCloseable {
          @Scoped
          private val r = CountingResource("level2")
          @Scoped
          private val child = Level3()
        }

        class Level1 : AutoCloseable {
          @Scoped
          private val r = CountingResource("level1")
          @Scoped
          private val child = Level2()
        }

        fun testMain() {
          Level1().close()
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(
        listOf("level1", "level2", "level3"),
      )
      assertThat(result.closed).isEqualTo(
        listOf("level3", "level2", "level1"),
      )
    }
  }

  @Nested
  inner class ExistingCloseMethod {

    @Test
    fun `should complement existing close method`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        class Root : AutoCloseable {
          @Scoped
          private val scopedResource = CountingResource("scopedResource")

          override fun close() {
            println("existing close logic")
          }
        }

        fun testMain() {
          Root().close()
        }
        """.trimIndent(),
      )

      assertThat(result.stdout).contains("existing close logic")
      assertThat(result.initialized).isEqualTo(listOf("scopedResource"))
      assertThat(result.closed).isEqualTo(listOf("scopedResource"))
    }

    @Test
    fun `should run existing close logic before scoped cleanup`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        class Root : AutoCloseable {
          @Scoped
          private val resource = CountingResource("resource")

          override fun close() {
            println("custom-cleanup")
          }
        }

        fun testMain() {
          Root().close()
        }
        """.trimIndent(),
      )

      val customIdx =
        result.stdout.indexOfFirst { it.contains("custom-cleanup") }
      val closeIdx =
        result.stdout.indexOfFirst { it.contains("closed") }
      assertThat(customIdx).isGreaterThanOrEqualTo(0)
      assertThat(closeIdx).isGreaterThanOrEqualTo(0)
      assertThat(customIdx).isLessThan(closeIdx)
    }

    @Test
    fun `should complement close method that already closes some resources`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        class Root : AutoCloseable {
          private val manual = CountingResource("manual")
          @Scoped
          private val scoped = CountingResource("scoped")

          override fun close() {
            manual.close()
          }
        }

        fun testMain() {
          Root().close()
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("manual", "scoped"))
      assertThat(result.closed).isEqualTo(listOf("manual", "scoped"))
    }
  }

  @Nested
  inner class ExceptionSafety {

    @Test
    fun `should close first property when second constructor throws`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource
        import kraii.util.FailingResource

        class Root : AutoCloseable {
          @Scoped
          private val a = CountingResource("a")
          @Scoped
          private val b = FailingResource()
        }

        fun testMain() {
          Root()
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("a"))
      assertThat(result.closed).isEqualTo(listOf("a"))
      assertThat(result.uncaughtException)
        .isEqualTo("java.lang.RuntimeException: constructor failed")
    }

    @Test
    fun `should propagate exception when close throws`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.FailingClose

        class Root : AutoCloseable {
          @Scoped
          private val a = FailingClose()
        }

        fun testMain() {
          Root().close()
        }
        """.trimIndent(),
      )

      assertThat(result.uncaughtException)
        .isEqualTo("java.lang.RuntimeException: close failed")
    }

    @Disabled("TODO")
    @Test
    fun `should close remaining properties when one close throws`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource
        import kraii.util.FailingClose

        class Root : AutoCloseable {
          @Scoped
          private val a = CountingResource("a")
          @Scoped
          private val b = FailingClose()
          @Scoped
          private val c = CountingResource("c")
        }

        fun testMain() {
          Root().close()
        }
        """.trimIndent(),
      )

      assertThat(result.closed).isEqualTo(listOf("c", "a"))
      assertThat(result.uncaughtException)
        .isEqualTo("java.lang.RuntimeException: close failed")
    }

    // When the user's close() body and a @Scoped close() both throw,
    // the body exception propagates and the close exception is added
    // via addSuppressed() (Java try-with-resources semantics).
    @Disabled("TODO")
    @Test
    fun `should suppress close exceptions and propagate original`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource
        import kraii.util.FailingClose

        class Root : AutoCloseable {
          @Scoped
          private val a = CountingResource("a")
          @Scoped
          private val b = FailingClose()

          override fun close() {
            throw RuntimeException("body failed")
          }
        }

        fun testMain() {
          try {
            Root().close()
          } catch (e: RuntimeException) {
            println("caught: ${'$'}{e.message}")
            e.suppressed.forEach { println("suppressed: ${'$'}{it.message}") }
          }
        }
        """.trimIndent(),
      )

      assertThat(result.closed).isEqualTo(listOf("a"))
      assertThat(result.stdout).contains("caught: body failed")
      assertThat(result.stdout).contains("suppressed: close failed")
    }

    @Disabled("TODO")
    @Test
    fun `should close all properties when multiple closes throw`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource
        import kraii.util.FailingClose

        class Root : AutoCloseable {
          @Scoped
          private val a = CountingResource("a")
          @Scoped
          private val b = FailingClose()
          @Scoped
          private val c = CountingResource("c")
          @Scoped
          private val d = FailingClose()
          @Scoped
          private val e = CountingResource("e")
        }

        fun testMain() {
          Root().close()
        }
        """.trimIndent(),
      )

      assertThat(result.closed).isEqualTo(listOf("e", "c", "a"))
    }
  }
}
