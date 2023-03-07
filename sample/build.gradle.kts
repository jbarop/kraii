plugins {
  kotlin("jvm") version "1.8.10"
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
