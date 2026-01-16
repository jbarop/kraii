allprojects {
  repositories {
    mavenCentral()
  }
}

plugins {
  // Applying external plugins with same version to subprojects
  kotlin("jvm") version "2.3.0" apply false
}
