package kraii

import kraii.util.compileAndRunTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class KraiiLocalVariableTest {

  @Disabled("Not yet implemented")
  @Test
  fun `should close scoped local variable at end of function`() {
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

  @Disabled("Not yet implemented")
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

  @Disabled("Not yet implemented")
  @Test
  fun `should close scoped local variable at end of enclosing block`() {
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

  @Disabled("Not yet implemented")
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

  @Disabled("Not yet implemented")
  @Test
  fun `should close scoped local variable on early return`() {
    val result = compileAndRunTest(
      """
      import kraii.api.Scoped
      import kraii.util.CountingResource

      fun doWork(): String {
        @Scoped val a = CountingResource("a")
        @Scoped val b = CountingResource("b")
        if (true) {
          return "early"
        }
        println("unreachable")
        @Scoped val c = CountingResource("c")
        return "late"
      }

      fun testMain() {
        doWork()
      }
      """.trimIndent(),
    )

    assertThat(result.initialized).isEqualTo(listOf("a", "b"))
    assertThat(result.closed).isEqualTo(listOf("b", "a"))
  }

  @Disabled("Not yet implemented")
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
}
