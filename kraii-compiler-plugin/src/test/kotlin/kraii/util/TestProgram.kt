package kraii.util

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import kotlin.io.path.createTempDirectory

/**
 * Compiles a Kotlin test program with the compiler-plugin and runs it.
 */
class TestProgram {

  private val sourcesDirectory =
    createTempDirectory("kraii-test-compilation-sources")
      .toFile()
      .also { it.deleteOnExit() }

  private val outputDirectory =
    createTempDirectory("kraii-test-compilation-output")
      .toFile()
      .also { it.deleteOnExit() }

  fun newSourceFile(
    fileName: String,
    fileContent: () -> String,
  ) {
    val file = sourcesDirectory.resolve(fileName)
    file.createNewFile()
    file.writeText(fileContent())
  }

  fun compile(): CompilationResult {
    val collector = CapturingMessageCollector()
    val kotlinCompiler = K2JVMCompiler()
    val exitCode = kotlinCompiler.exec(
      messageCollector = collector,
      services = Services.EMPTY,
      arguments = kotlinCompiler.createArguments().also { args ->
        args.noStdlib = true
        args.noReflect = true
        args.disableDefaultScriptingPlugin = true
        args.jdkHome = System.getProperty("java.home")
        args.classpath = System.getProperty("java.class.path")
        args.pluginClasspaths =
          System.getProperty("java.class.path").split(":").toTypedArray()
        args.destination = outputDirectory.absolutePath
        args.freeArgs = listOf(sourcesDirectory.absolutePath)
      },
    )

    return CompilationResult(
      success = exitCode == ExitCode.OK,
      errors = collector.errors,
    )
  }

  fun execute(mainClass: String): List<String> {
    val javaExecutable = ProcessHandle
      .current()
      .info()
      .command()
      .get()
    val classPath = System
      .getProperty("java.class.path")
      .split(":")
      .plus(outputDirectory)
      .joinToString(":")
    val process = ProcessBuilder(
      javaExecutable,
      "-cp",
      classPath,
      mainClass,
    ).start()
    val returnCode = process.waitFor()
    if (returnCode != 0) {
      System.err.println(process.errorStream.bufferedReader().readText())
      error("Test execution failed with return code `$returnCode`.")
    }

    return process.inputStream.bufferedReader().readLines()
  }
}

data class CompilationResult(
  val success: Boolean,
  val errors: List<String>,
)

private class CapturingMessageCollector : MessageCollector {

  val errors = mutableListOf<String>()

  override fun clear() {
    errors.clear()
  }

  override fun hasErrors(): Boolean = errors.isNotEmpty()

  override fun report(
    severity: CompilerMessageSeverity,
    message: String,
    location: CompilerMessageSourceLocation?,
  ) {
    if (severity.isError) {
      errors.add(message)
    }
  }
}
