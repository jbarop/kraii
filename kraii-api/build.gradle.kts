group = "kraii"

plugins {
  `java-library`
  id("org.jetbrains.kotlin.jvm") version "1.7.21"
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}
