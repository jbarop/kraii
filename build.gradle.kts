import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension

allprojects {
  repositories {
    mavenCentral()
  }
}

plugins {
  // Applying external plugins with same version to subprojects
  kotlin("jvm") version "1.8.10" apply false
}

subprojects {
  plugins.withId("org.jetbrains.kotlin.jvm") {
    extensions.configure<KotlinTopLevelExtension> {
      jvmToolchain {
        jvmToolchain(17)
      }
    }
  }
}
