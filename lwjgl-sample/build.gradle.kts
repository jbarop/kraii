import org.gradle.internal.os.OperatingSystem

plugins {
  id("org.jetbrains.kotlin.jvm") version "1.7.21"
  id("kraii-gradle-plugin")
  application
}

val operatingSystem: OperatingSystem = OperatingSystem.current()
val lwjglVersion = "3.3.1"
val lwjglNatives = when {
  operatingSystem.isWindows -> "natives-windows"
  operatingSystem.isLinux -> "natives-linux"
  operatingSystem.isMacOsX -> "natives-macos"
  else -> error("Unsupported operating system: $operatingSystem")
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

  implementation("org.lwjgl:lwjgl:$lwjglVersion")
  runtimeOnly("org.lwjgl:lwjgl:${lwjglVersion}:$lwjglNatives")

  implementation("org.lwjgl:lwjgl-glfw:$lwjglVersion")
  runtimeOnly("org.lwjgl:lwjgl-glfw:${lwjglVersion}:$lwjglNatives")

  implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion")
  runtimeOnly("org.lwjgl:lwjgl-opengl:${lwjglVersion}:$lwjglNatives")
}

application {
  mainClass.set("kraii.sample.LwjglSampleKt")
}
