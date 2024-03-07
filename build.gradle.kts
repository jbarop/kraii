import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension

allprojects {
  repositories {
    mavenCentral()
  }
}

plugins {
  // Applying external plugins with same version to subprojects
  kotlin("jvm") version "1.9.23" apply false
}
