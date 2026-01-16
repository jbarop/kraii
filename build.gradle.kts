allprojects {
  repositories {
    mavenCentral()
  }
}

plugins {
  // Applying external plugins with same version to subprojects
  alias(libs.plugins.kotlin.jvm) apply false
}
