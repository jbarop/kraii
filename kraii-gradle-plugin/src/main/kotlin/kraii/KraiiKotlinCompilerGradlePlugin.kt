package kraii

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

/**
 * Adapter between Gradle and the Kotlin compiler plugin.
 */
class KraiiKotlinCompilerGradlePlugin : KotlinCompilerPluginSupportPlugin {

  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean =
    kotlinCompilation.target.project.plugins.hasPlugin(KraiiKotlinCompilerGradlePlugin::class.java)

  override fun getCompilerPluginId(): String = "kraii"

  override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
    groupId = "kraii",
    artifactId = "kraii-compiler-plugin",
    version = "0.0.1",
  )

  override fun apply(target: Project) {
    target.extensions.create(
      /* name = */ "kraii",
      /* type = */ KraiiGradleExtension::class.java,
    )
  }

  override fun applyToCompilation(
    kotlinCompilation: KotlinCompilation<*>,
  ): Provider<List<SubpluginOption>> =
    kotlinCompilation.target.project.provider { emptyList() }
}

open class KraiiGradleExtension
