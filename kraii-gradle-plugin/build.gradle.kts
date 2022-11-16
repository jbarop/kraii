plugins {
  `java-gradle-plugin`
  id("org.jetbrains.kotlin.jvm") version "1.6.21"
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

gradlePlugin {
  val greeting by plugins.creating {
    id = "kraii-gradle-plugin"
    implementationClass = "kraii.KraiiPlugin"
  }
}
