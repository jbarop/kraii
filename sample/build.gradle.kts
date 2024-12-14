plugins {
  kotlin("jvm") version "1.9.21"
  id("kraii-gradle-plugin")
  application
}

repositories {
  mavenCentral()
}

application {
  mainClass.set("kraii.sample.AppKt")
}

tasks.named<JavaExec>("run") {
  standardInput = System.`in`
}
