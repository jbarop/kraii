allprojects {
  repositories {
    mavenCentral()
  }
}

plugins {
  // Applying external plugins with same version to subprojects
  kotlin("jvm") version "2.0.21" apply false
}
