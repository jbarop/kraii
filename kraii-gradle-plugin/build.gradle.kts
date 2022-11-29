plugins {
  `java-gradle-plugin`
  kotlin("jvm")
}

dependencies {
  compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin-api")
}

gradlePlugin {
  val kraii by plugins.creating {
    id = "kraii-gradle-plugin"
    implementationClass = "kraii.KraiiKotlinCompilerGradlePlugin"
  }
}
