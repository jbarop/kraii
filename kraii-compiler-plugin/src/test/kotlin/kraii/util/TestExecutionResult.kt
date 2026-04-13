package kraii.util

import org.junit.jupiter.api.fail

data class TestExecutionResult(

  /**
   * Standard output from the test program.
   */
  val stdout: List<String>,

  /**
   * Names of the [CountingResource]s which were initialized.
   */
  val initialized: List<String>,

  /**
   * Names of the [CountingResource]s which were closed.
   */
  val closed: List<String>,

  /**
   * The uncaught exception thrown by `testMain()`, if any.
   */
  val uncaughtException: String?,
)

/**
 * Compiles and runs the given Kotlin source.
 *
 * The [testSourceContent] must provide a `testMain()` method which will be invoked during the test.
 */
fun compileAndRunTest(testSourceContent: String): TestExecutionResult {
  val program = TestProgram()

  program.newSourceFile("main.kt") {
    """
    import kraii.util.CountingResource

    fun main() {
      var uncaughtException: Throwable? = null
      try {
        testMain()
      } catch (e: Throwable) {
        uncaughtException = e
      }
      println(CountingResource.serialize())
      if (uncaughtException != null) {
        println("UncaughtException=${'$'}{uncaughtException::class.qualifiedName}: ${'$'}{uncaughtException.message}")
      }
    }
    """.trimIndent()
  }
  program.newSourceFile("TestCase.kt") { testSourceContent }

  val compilationResult = program.compile()
  if (!compilationResult.success) {
    fail {
      "Compilation of test source failed:\n" +
        compilationResult.errors.joinToString("\n")
    }
  }

  val lines = program.execute("MainKt")
  val countingResourceStatus = CountingResource.deserialize(lines)
  val uncaughtException = lines
    .find { it.startsWith("UncaughtException=") }
    ?.removePrefix("UncaughtException=")
  return TestExecutionResult(
    stdout = lines,
    initialized = countingResourceStatus.initialized,
    closed = countingResourceStatus.closed,
    uncaughtException = uncaughtException,
  )
}

/**
 * Compiles the given Kotlin source with the plugin active and returns
 * the [CompilationResult].
 */
fun compile(testSourceContent: String): CompilationResult {
  val program = TestProgram()
  program.newSourceFile("TestCase.kt") { testSourceContent }
  return program.compile()
}
