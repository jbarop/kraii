allprojects {
  repositories {
    mavenCentral()
  }
}

plugins {
  // Applying external plugins with same version to subprojects
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.ktlint) apply false
}

subprojects {
  pluginManager.withPlugin("org.jlleitschuh.gradle.ktlint") {
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
      version.set(rootProject.libs.versions.ktlint.engine)
    }
  }
}
