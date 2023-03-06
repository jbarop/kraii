package kraii

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector.Companion.NONE
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

@OptIn(ExperimentalCompilerApi::class)
class KraiiCompilerPluginRegistrar : CompilerPluginRegistrar() {

  override val supportsK2: Boolean = true

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    val messageCollector = configuration.get(MESSAGE_COLLECTOR_KEY, NONE)
    messageCollector.report(INFO, "kRAII Kotlin plugin active")
    SyntheticResolveExtension.registerExtension(KraiiSyntheticResolveExtension())
    IrGenerationExtension.registerExtension(KraiiIrGenerationExtension(messageCollector))
  }
}
