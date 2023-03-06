plugins {
  kotlin("jvm") version "1.8.10"
  id("kraii-gradle-plugin")
  application
}

repositories {
  mavenCentral()
}

application {
  mainClass.set("kraii.sample.AppKt")
}
