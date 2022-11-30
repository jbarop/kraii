package kraii.util

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import kraii.KraiiComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar

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
 * The source should provide a `testMain()` method which will be invoked when the test is executed.
 */
fun compileAndRunTest(testSourceContent: String): TestExecutionResult =
  compile(
    SourceFile.kotlin(
      name = "main.kt",
      contents = """
      import kraii.util.CountingResource
      
      fun main() {
        testMain()
        println(CountingResource.serialize())
      }
      """.trimIndent()
    ),
    SourceFile.kotlin(
      name = "testSource.kt",
      contents = testSourceContent,
    ),
  ).runTest()

private fun compile(vararg testSources: SourceFile): KotlinCompilation.Result {
  val compilationResult = KotlinCompilation().apply {
    sources = testSources.toList()
    useIR = true
    compilerPlugins = listOf<ComponentRegistrar>(KraiiComponentRegistrar())
    inheritClassPath = true
  }.compile()

  if (compilationResult.exitCode != KotlinCompilation.ExitCode.OK) {
    error("Kotlin Compilation failed.")
  }

  return compilationResult
}

private fun KotlinCompilation.Result.runTest(): TestExecutionResult {
  val javaExecutable = ProcessHandle.current().info().command().get()
  val classPath = System.getProperty("java.class.path")
    .split(":")
    .plus(outputDirectory)
    .joinToString(":")
  val process = ProcessBuilder(javaExecutable, "-cp", classPath, "MainKt").start()
  val returnCode = process.waitFor()
  if (returnCode != 0) {
    System.err.println(process.errorStream.bufferedReader().readText())
    error("Test execution failed with return code `$returnCode`.")
  }

  return process.parseTestExecutionResult()
}

private fun Process.parseTestExecutionResult(): TestExecutionResult {
  val lines = inputStream.bufferedReader().readLines()
  val countingResourceStatus = CountingResource.deserialize(lines)
  return TestExecutionResult(
    initialized = countingResourceStatus.initialized,
    closed = countingResourceStatus.closed,
  )
}
