package kraii.util

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer.PLAIN_FULL_PATHS
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import kotlin.io.path.createTempDirectory

/**
 * Compiles a Kotlin test program with the compiler-plugin and runs it.
 */
class TestProgram {

  private val sourcesDirectory =
    createTempDirectory("kraii-test-compilation-sources").toFile()
      .also { it.deleteOnExit() }

  private val outputDirectory =
    createTempDirectory("kraii-test-compilation-output").toFile()
      .also { it.deleteOnExit() }

  fun newSourceFile(fileName: String, fileContent: () -> String) {
    val file = sourcesDirectory.resolve(fileName)
    file.createNewFile()
    file.writeText(fileContent())
  }

  fun compile(): Boolean {
    val kotlinCompiler = K2JVMCompiler()
    val result = kotlinCompiler.exec(
      messageCollector = PrintingMessageCollector(System.out, PLAIN_FULL_PATHS, true),
      services = Services.EMPTY,
      arguments = kotlinCompiler.createArguments().also { args ->
        args.noStdlib = true
        args.noReflect = true
        args.disableDefaultScriptingPlugin = true
        args.jdkHome = System.getProperty("java.home")
        args.classpath = System.getProperty("java.class.path")
        args.pluginClasspaths = System.getProperty("java.class.path").split(":").toTypedArray()
        args.destination = outputDirectory.absolutePath
        args.freeArgs = listOf(sourcesDirectory.absolutePath)
      }
    )

    return result == ExitCode.OK
  }

  fun execute(mainClass: String): List<String> {
    val javaExecutable = ProcessHandle.current().info().command().get()
    val classPath = System.getProperty("java.class.path")
      .split(":")
      .plus(outputDirectory)
      .joinToString(":")
    val process = ProcessBuilder(javaExecutable, "-cp", classPath, mainClass).start()
    val returnCode = process.waitFor()
    if (returnCode != 0) {
      System.err.println(process.errorStream.bufferedReader().readText())
      error("Test execution failed with return code `$returnCode`.")
    }

    return process.inputStream.bufferedReader().readLines()
  }

}
