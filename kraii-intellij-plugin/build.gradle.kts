plugins {
  kotlin("jvm")
  id("org.jetbrains.intellij") version "1.13.1"
}

dependencies {
  implementation(project(":kraii-compiler-plugin"))
}

intellij {
  pluginName.set("KRAII")

  version.set("2022.2.3")
  type.set("IC")
  plugins.set(listOf("org.jetbrains.kotlin"))
}

tasks {
  buildSearchableOptions {
    enabled = false
  }
}
