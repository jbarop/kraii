allprojects {
  repositories {
    mavenCentral()
  }
}

plugins {
  // Applying external plugins with same version to subprojects
  kotlin("jvm") version "1.8.10" apply false
}
