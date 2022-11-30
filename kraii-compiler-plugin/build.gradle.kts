plugins {
  kotlin("jvm")
}

dependencies {
  compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable")

  testImplementation(platform("org.junit:junit-bom:5.9.1"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  implementation("org.assertj:assertj-core:3.23.1")
  testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.9")
  testImplementation(project(":kraii-api"))
}

tasks.test {
  useJUnitPlatform()
}
