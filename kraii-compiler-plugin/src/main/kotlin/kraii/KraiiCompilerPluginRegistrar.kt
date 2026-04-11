package kraii

import kraii.fir.KraiiFirExtensionRegistrar
import kraii.ir.KraiiIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO
import org.jetbrains.kotlin.cli.common.messages.MessageCollector.Companion.NONE
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

class KraiiCompilerPluginRegistrar : CompilerPluginRegistrar() {

  override val pluginId: String = "kraii"

  override val supportsK2: Boolean = true

  override fun ExtensionStorage.registerExtensions(
    configuration: CompilerConfiguration,
  ) {
    val messageCollector = configuration.get(MESSAGE_COLLECTOR_KEY, NONE)
    messageCollector.report(INFO, "kRAII Kotlin plugin active")
    registerExtension(
      FirExtensionRegistrarAdapter,
      KraiiFirExtensionRegistrar(),
    )
    registerExtension(
      IrGenerationExtension,
      KraiiIrGenerationExtension(),
    )
  }
}

/**
 * Registers a compiler plugin extension via reflection to avoid a
 * binary incompatibility between the public Kotlin compiler and
 * IntelliJ's bundled analysis engine.
 *
 * See https://youtrack.jetbrains.com/issue/KTIJ-38372
 */
private fun CompilerPluginRegistrar.ExtensionStorage.registerExtension(
  descriptor: Any,
  extension: Any,
) {
  val method = this::class.java.methods
    .first { it.name == "registerExtension" }
  method.invoke(this, descriptor, extension)
}
