plugins {
  id("org.jetbrains.kotlin.jvm") version "1.6.21"
  id("kraii-gradle-plugin")
  application
}

repositories {
  // Use Maven Central for resolving dependencies.
  mavenCentral()
}

dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

application {
  mainClass.set("kraii.sample.AppKt")
}
