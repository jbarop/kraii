import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension

allprojects {
  repositories {
    mavenCentral()
  }
}

plugins {
  // Applying external plugins with same version to subprojects
  kotlin("jvm") version "2.1.20" apply false
}
