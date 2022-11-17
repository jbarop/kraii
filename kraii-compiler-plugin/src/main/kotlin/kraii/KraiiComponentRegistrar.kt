package kraii

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.WARNING
import org.jetbrains.kotlin.cli.common.messages.MessageCollector.Companion.NONE
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration

class KraiiComponentRegistrar : ComponentRegistrar {

  override fun registerProjectComponents(
    project: MockProject,
    configuration: CompilerConfiguration,
  ) {
    val messageCollector = configuration.get(MESSAGE_COLLECTOR_KEY, NONE)

    configuration.kotlinSourceRoots.forEach {
      messageCollector.report(WARNING, "---- Hi ${it.path}")
    }
  }

}
