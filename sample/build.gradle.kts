plugins {
  kotlin("jvm") version "1.9.21"
  id("kraii-gradle-plugin")
  application
}

repositories {
  mavenCentral()
}

kotlin {
  jvmToolchain(17)
}

application {
  mainClass.set("kraii.sample.AppKt")
}
