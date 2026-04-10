package kraii

import kraii.util.compileAndRunTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class KraiiLocalVariableTest {

  @Nested
  inner class FunctionScope {

    @Test
    fun `should close scoped local variable`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          @Scoped val resource = CountingResource("a")
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("a"))
      assertThat(result.closed).isEqualTo(listOf("a"))
    }

    @Test
    fun `should close multiple scoped local variables in reverse order`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          @Scoped val a = CountingResource("a")
          @Scoped val b = CountingResource("b")
          @Scoped val c = CountingResource("c")
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("a", "b", "c"))
      assertThat(result.closed).isEqualTo(listOf("c", "b", "a"))
    }

    @Test
    fun `should not close non-scoped local variables`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          val notScoped = CountingResource("notScoped")
          @Scoped val scoped = CountingResource("scoped")
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("notScoped", "scoped"))
      assertThat(result.closed).isEqualTo(listOf("scoped"))
    }

    @Test
    fun `should allow statements between declaration and close`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          @Scoped val a = CountingResource("a")
          println("using resource a")
          @Scoped val b = CountingResource("b")
          println("using resource b")
          @Scoped val c = CountingResource("c")
          println("using resource c")
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("a", "b", "c"))
      assertThat(result.closed).isEqualTo(listOf("c", "b", "a"))
      assertThat(result.stdout.indexOf("using resource a"))
        .isLessThan(result.stdout.indexOfFirst { it.contains("closed") })
    }

    @Test
    fun `should preserve return value`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun compute(): Int {
          @Scoped val r = CountingResource("r")
          return 42
        }

        fun testMain() {
          val value = compute()
          println("result=${'$'}value")
        }
        """.trimIndent(),
      )

      assertThat(result.closed).isEqualTo(listOf("r"))
      assertThat(result.stdout).contains("result=42")
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

        fun testMain() {
          @Scoped val container = listOf(
            CountingResource("resource"),
          )
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("resource"))
      assertThat(result.closed).isEqualTo(listOf("resource"))
    }

    @Test
    fun `should close resources in container in reverse element order`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          @Scoped val container = listOf(
            CountingResource("a"),
            CountingResource("b"),
            CountingResource("c"),
          )
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

        fun testMain() {
          @Scoped val container1 = listOf(
            CountingResource("c1-a"),
            CountingResource("c1-b"),
          )
          @Scoped val container2 = listOf(
            CountingResource("c2-a"),
            CountingResource("c2-b"),
          )
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
    fun `should handle mixed direct variables and containers`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          @Scoped val direct = CountingResource("direct")
          @Scoped val container = listOf(
            CountingResource("in-container-a"),
            CountingResource("in-container-b"),
          )
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
  inner class BlockScope {

    @Test
    fun `should close scoped local variable`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          @Scoped val outer = CountingResource("outer")
          if (true) {
            @Scoped val inner = CountingResource("inner")
          }
          println("after-if")
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("outer", "inner"))
      assertThat(result.closed).isEqualTo(listOf("inner", "outer"))
      assertThat(result.stdout).contains("after-if")
    }

    @Test
    fun `should close multiple scoped local variables in reverse order`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          @Scoped val outer1 = CountingResource("outer-1")
          if (true) {
            @Scoped val inner1 = CountingResource("inner-1")
            @Scoped val inner2 = CountingResource("inner-2")
          }
          println("after-if")
          @Scoped val outer2 = CountingResource("outer-2")
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(
        listOf(
          "outer-1",
          "inner-1",
          "inner-2",
          "outer-2",
        ),
      )
      assertThat(result.closed).isEqualTo(
        listOf(
          "inner-2",
          "inner-1",
          "outer-2",
          "outer-1",
        ),
      )
      assertThat(result.stdout).contains("after-if")
    }

    @Test
    fun `should handle deeply nested blocks`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          @Scoped val a = CountingResource("a")
          if (true) {
            @Scoped val b = CountingResource("b")
            if (true) {
              @Scoped val c = CountingResource("c")
            }
          }
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("a", "b", "c"))
      assertThat(result.closed).isEqualTo(listOf("c", "b", "a"))
    }

    @Test
    fun `should handle when expression with scoped vars in branches`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          val x = 2
          @Scoped val outer = CountingResource("outer")
          when (x) {
            1 -> {
              @Scoped val branch1 = CountingResource("branch1")
            }
            2 -> {
              @Scoped val branch2 = CountingResource("branch2")
            }
            else -> {
              @Scoped val branchElse = CountingResource("branchElse")
            }
          }
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("outer", "branch2"))
      assertThat(result.closed).isEqualTo(listOf("branch2", "outer"))
    }

    @Test
    fun `should only close scoped var in else branch when else is taken`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          if (false) {
            @Scoped val ifBranch = CountingResource("ifBranch")
          } else {
            @Scoped val elseBranch = CountingResource("elseBranch")
          }
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("elseBranch"))
      assertThat(result.closed).isEqualTo(listOf("elseBranch"))
    }
  }

  @Nested
  inner class Loops {

    @Test
    fun `should close scoped local variable on each loop iteration`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          for (i in 1..3) {
            @Scoped val r = CountingResource("r${'$'}i")
          }
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("r1", "r2", "r3"))
      assertThat(result.closed).isEqualTo(listOf("r1", "r2", "r3"))
    }

    @Test
    fun `should close scoped var on each while loop iteration`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          var i = 0
          while (i < 3) {
            @Scoped val r = CountingResource("r${'$'}{i}")
            i++
          }
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("r0", "r1", "r2"))
      assertThat(result.closed).isEqualTo(listOf("r0", "r1", "r2"))
    }

    @Test
    fun `should close scoped var on break`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          for (i in 1..5) {
            @Scoped val r = CountingResource("r${'$'}i")
            if (i == 2) break
          }
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("r1", "r2"))
      assertThat(result.closed).isEqualTo(listOf("r1", "r2"))
    }

    @Test
    fun `should close scoped var on continue`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          for (i in 1..3) {
            @Scoped val r = CountingResource("r${'$'}i")
            if (i == 2) continue
            println("used ${'$'}i")
          }
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("r1", "r2", "r3"))
      assertThat(result.closed).isEqualTo(listOf("r1", "r2", "r3"))
      assertThat(result.stdout).contains("used 1")
      assertThat(result.stdout).doesNotContain("used 2")
      assertThat(result.stdout).contains("used 3")
    }
  }

  @Nested
  inner class EarlyReturn {

    @Test
    fun `should close scoped local variables on first return`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun doWork(path: Int): String {
          @Scoped val a = CountingResource("a")
          if (path == 1) {
            return "first"
          }
          @Scoped val b = CountingResource("b")
          if (path == 2) {
            return "second"
          }
          @Scoped val c = CountingResource("c")
          return "third"
        }

        fun testMain() {
          doWork(1)
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("a"))
      assertThat(result.closed).isEqualTo(listOf("a"))
    }

    @Test
    fun `should close scoped local variables on second return`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun doWork(path: Int): String {
          @Scoped val a = CountingResource("a")
          if (path == 1) {
            return "first"
          }
          @Scoped val b = CountingResource("b")
          if (path == 2) {
            return "second"
          }
          @Scoped val c = CountingResource("c")
          return "third"
        }

        fun testMain() {
          doWork(2)
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("a", "b"))
      assertThat(result.closed).isEqualTo(listOf("b", "a"))
    }

    @Test
    fun `should close scoped local variables on third return`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun doWork(path: Int): String {
          @Scoped val a = CountingResource("a")
          if (path == 1) {
            return "first"
          }
          @Scoped val b = CountingResource("b")
          if (path == 2) {
            return "second"
          }
          @Scoped val c = CountingResource("c")
          return "third"
        }

        fun testMain() {
          doWork(3)
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("a", "b", "c"))
      assertThat(result.closed).isEqualTo(listOf("c", "b", "a"))
    }

    @Test
    fun `should close scoped vars from nested block on return`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun doWork(flag: Boolean): String {
          @Scoped val outer = CountingResource("outer")
          if (flag) {
            @Scoped val inner = CountingResource("inner")
            return "early"
          }
          return "normal"
        }

        fun testMain() {
          doWork(true)
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("outer", "inner"))
      assertThat(result.closed).isEqualTo(listOf("inner", "outer"))
    }
  }

  @Nested
  inner class ExceptionSafety {

    @Test
    fun `should close scoped local variable when exception is thrown`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          try {
            @Scoped val a = CountingResource("a")
            @Scoped val b = CountingResource("b")
            throw RuntimeException("boom")
            @Scoped val c = CountingResource("c")
          } catch (e: RuntimeException) {
            println("caught: ${'$'}{e.message}")
          }
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("a", "b"))
      assertThat(result.closed).isEqualTo(listOf("b", "a"))
      assertThat(result.stdout).contains("caught: boom")
    }

    @Test
    fun `should close scoped locals on uncaught exception`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          @Scoped val a = CountingResource("a")
          @Scoped val b = CountingResource("b")
          throw RuntimeException("boom")
          @Scoped val c = CountingResource("c")
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("a", "b"))
      assertThat(result.closed).isEqualTo(listOf("b", "a"))
      assertThat(
        result.uncaughtException,
      ).isEqualTo("java.lang.RuntimeException: boom")
    }

    @Test
    fun `should close first resource when second constructor throws`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource
        import kraii.util.FailingResource

        fun testMain() {
          @Scoped val a = CountingResource("a")
          @Scoped val b = FailingResource()
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("a"))
      assertThat(result.closed).isEqualTo(listOf("a"))
      assertThat(
        result.uncaughtException,
      ).isEqualTo("java.lang.RuntimeException: constructor failed")
    }

    @Test
    fun `should propagate exception when close throws`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.FailingClose

        fun testMain() {
          @Scoped val a = FailingClose()
        }
        """.trimIndent(),
      )

      assertThat(
        result.uncaughtException,
      ).isEqualTo("java.lang.RuntimeException: close failed")
    }

    @Test
    fun `should propagate body exception when close also throws`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource
        import kraii.util.FailingClose

        fun testMain() {
          @Scoped val a = FailingClose()
          throw RuntimeException("body failed")
        }
        """.trimIndent(),
      )

      // JVM try-finally semantics: finally exception replaces try exception
      assertThat(
        result.uncaughtException,
      ).isEqualTo("java.lang.RuntimeException: close failed")
    }

    @Test
    fun `should close all scoped vars across nested scopes on exception`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource
        import kraii.util.FailingResource

        fun testMain() {
          @Scoped val top = CountingResource("top")
          for (i in 1..3) {
            @Scoped val loop = CountingResource("loop${'$'}i")
            if (i == 2) {
              @Scoped val nested = CountingResource("nested")
              run {
                @Scoped val inRun = CountingResource("inRun")
                @Scoped val failing = FailingResource()
              }
            }
          }
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(
        listOf("top", "loop1", "loop2", "nested", "inRun"),
      )
      assertThat(result.closed).isEqualTo(
        listOf("loop1", "inRun", "nested", "loop2", "top"),
      )
      assertThat(
        result.uncaughtException,
      ).isEqualTo("java.lang.RuntimeException: constructor failed")
    }

    @Test
    fun `should close Iterable container when later constructor throws`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource
        import kraii.util.FailingResource

        fun testMain() {
          @Scoped val container = listOf(
            CountingResource("a"),
            CountingResource("b"),
          )
          @Scoped val failing = FailingResource()
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("a", "b"))
      assertThat(result.closed).isEqualTo(listOf("b", "a"))
      assertThat(
        result.uncaughtException,
      ).isEqualTo("java.lang.RuntimeException: constructor failed")
    }

    @Test
    fun `should close remaining elements in container when one close throws`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource
        import kraii.util.FailingClose

        fun testMain() {
          @Scoped val container: List<AutoCloseable> = listOf(
            CountingResource("a"),
            FailingClose(),
            CountingResource("c"),
          )
        }
        """.trimIndent(),
      )

      assertThat(result.closed).isEqualTo(listOf("c", "a"))
      assertThat(
        result.uncaughtException,
      ).isEqualTo("java.lang.RuntimeException: close failed")
    }
  }

  @Nested
  inner class Lambdas {

    @Test
    fun `should close scoped var inside lambda`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          val action = {
            @Scoped val r = CountingResource("lambda")
          }
          action()
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("lambda"))
      assertThat(result.closed).isEqualTo(listOf("lambda"))
    }

    @Test
    fun `should close scoped var inside run block`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          @Scoped val outer = CountingResource("outer")
          run {
            @Scoped val inner = CountingResource("inner")
          }
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("outer", "inner"))
      assertThat(result.closed).isEqualTo(listOf("inner", "outer"))
    }

    @Test
    fun `should close scoped var inside forEach lambda`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          listOf(1, 2, 3).forEach { i ->
            @Scoped val r = CountingResource("r${'$'}i")
          }
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("r1", "r2", "r3"))
      assertThat(result.closed).isEqualTo(listOf("r1", "r2", "r3"))
    }

    @Test
    fun `should close scoped var on return@forEach`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          listOf(1, 2, 3).forEach { i ->
            @Scoped val r = CountingResource("r${'$'}i")
            if (i == 2) return@forEach
            println("used ${'$'}i")
          }
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("r1", "r2", "r3"))
      assertThat(result.closed).isEqualTo(listOf("r1", "r2", "r3"))
      assertThat(result.stdout).contains("used 1")
      assertThat(result.stdout).doesNotContain("used 2")
      assertThat(result.stdout).contains("used 3")
    }

    @Test
    fun `should close scoped var on return@run`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          @Scoped val outer = CountingResource("outer")
          val result = run {
            @Scoped val inner = CountingResource("inner")
            return@run "done"
          }
          println("result=${'$'}result")
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("outer", "inner"))
      assertThat(result.closed).isEqualTo(listOf("inner", "outer"))
      assertThat(result.stdout).contains("result=done")
    }

    @Test
    fun `should close scoped var on return@let`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          val result = "input".let {
            @Scoped val r = CountingResource("r")
            return@let it.uppercase()
          }
          println("result=${'$'}result")
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("r"))
      assertThat(result.closed).isEqualTo(listOf("r"))
      assertThat(result.stdout).contains("result=INPUT")
    }
  }

  @Nested
  inner class LabeledReturns {

    @Test
    fun `should close scoped var on break@label`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          @Scoped val top = CountingResource("top")
          outer@ for (i in 1..3) {
            @Scoped val a = CountingResource("a${'$'}i")
            for (j in 1..3) {
              @Scoped val b = CountingResource("b${'$'}i-${'$'}j")
              for (k in 1..3) {
                @Scoped val c = CountingResource("c${'$'}i-${'$'}j-${'$'}k")
                if (i == 1 && j == 2 && k == 1) break@outer
              }
            }
          }
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(
        listOf(
          "top",
          "a1",
          "b1-1",
          "c1-1-1",
          "c1-1-2",
          "c1-1-3",
          "b1-2",
          "c1-2-1",
        ),
      )
      assertThat(result.closed).isEqualTo(
        listOf(
          "c1-1-1",
          "c1-1-2",
          "c1-1-3",
          "b1-1",
          "c1-2-1",
          "b1-2",
          "a1",
          "top",
        ),
      )
    }

    @Test
    fun `should close scoped var on continue@label`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          outer@ for (i in 1..3) {
            @Scoped val r = CountingResource("r${'$'}i")
            if (i == 2) continue@outer
            println("used ${'$'}i")
          }
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(listOf("r1", "r2", "r3"))
      assertThat(result.closed).isEqualTo(listOf("r1", "r2", "r3"))
      assertThat(result.stdout).contains("used 1")
      assertThat(result.stdout).doesNotContain("used 2")
      assertThat(result.stdout).contains("used 3")
    }

    @Test
    fun `should close scoped vars on break@label from nested loop`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun testMain() {
          @Scoped val top = CountingResource("top")
          outer@ for (i in 1..3) {
            @Scoped val outer = CountingResource("outer${'$'}i")
            for (j in 1..3) {
              @Scoped val inner = CountingResource("inner${'$'}i-${'$'}j")
              if (i == 1 && j == 2) break@outer
            }
          }
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(
        listOf("top", "outer1", "inner1-1", "inner1-2"),
      )
      assertThat(result.closed).isEqualTo(
        listOf("inner1-1", "inner1-2", "outer1", "top"),
      )
    }

    @Test
    fun `should close scoped var on return from lambda inside function`() {
      val result = compileAndRunTest(
        """
        import kraii.api.Scoped
        import kraii.util.CountingResource

        fun doWork(): String {
          @Scoped val outer = CountingResource("outer")
          listOf(1, 2, 3).forEach { i ->
            @Scoped val inner = CountingResource("inner${'$'}i")
            if (i == 2) return "early"
          }
          return "normal"
        }

        fun testMain() {
          val result = doWork()
          println("result=${'$'}result")
        }
        """.trimIndent(),
      )

      assertThat(result.initialized).isEqualTo(
        listOf("outer", "inner1", "inner2"),
      )
      assertThat(result.closed).isEqualTo(
        listOf("inner1", "inner2", "outer"),
      )
      assertThat(result.stdout).contains("result=early")
    }
  }
}
