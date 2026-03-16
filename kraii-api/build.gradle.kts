group = "kraii"

plugins {
  `java-library`
  kotlin("jvm")
  alias(libs.plugins.ktlint)
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}
