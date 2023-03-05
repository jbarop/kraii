package kraii.util

import org.junit.jupiter.api.fail

data class TestExecutionResult(

  /**
   * Names of the [CountingResource]s which were initialized.
   */
  val initialized: List<String>,

  /**
   * Names of the [CountingResource]s which were closed.
   */
  val closed: List<String>,
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
        testMain()
        println(CountingResource.serialize())
      }
    """.trimIndent()
  }
  program.newSourceFile("TestCase.kt") { testSourceContent }

  if (!program.compile()) {
    fail { "Compilation of test source failed :-(" }
  }

  val lines = program.execute("MainKt")
  val countingResourceStatus = CountingResource.deserialize(lines)
  return TestExecutionResult(
    initialized = countingResourceStatus.initialized,
    closed = countingResourceStatus.closed,
  )
}
