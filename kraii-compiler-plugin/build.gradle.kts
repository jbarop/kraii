plugins {
  `java-gradle-plugin`
  id("org.jetbrains.kotlin.jvm") version "1.6.21"
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

  compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable")

  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
  testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.9")
}
