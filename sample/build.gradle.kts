plugins {
  kotlin("jvm") version "2.3.0"
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
