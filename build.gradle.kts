allprojects {
  repositories {
    mavenCentral()
  }
}

plugins {
  // Applying external plugins with same version to subprojects
  kotlin("jvm") version "2.1.21" apply false
}
