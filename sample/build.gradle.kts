plugins {
  kotlin("jvm") version "2.3.20"
  id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
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
