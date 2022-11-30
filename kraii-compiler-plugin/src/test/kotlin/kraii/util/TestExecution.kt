package kraii.util

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import kraii.KraiiComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar

data class TestExecutionResult(

  /**
   * How many times [CountingResource] was initialized.
   */
  val numInitialized: Int,

  /**
   * How many times [CountingResource] was closed.
   */
  val numClosed: Int,
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
        println("numInitialized=" + CountingResource.numInitialized)
        println("numClosed=" + CountingResource.numClosed)
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
    .filter { it.contains("kraii") || it.contains("kotlin-stdlib") }
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
  return TestExecutionResult(
    numInitialized = lines.parse("numInitialized"),
    numClosed = lines.parse("numClosed"),
  )
}

private fun List<String>.parse(property: String): Int {
  val line = find { it.startsWith("$property=") } ?: error("'$property' not found in output.")
  val split = line.split("=")
  if (split.size != 2) error("Cannot parse '$line'.")
  return split[1].toIntOrNull() ?: error("Cannot parse number from '$line'.")
}
