plugins {
  kotlin("jvm")
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
  implementation(project(":kraii-api"))

  testImplementation(platform("org.junit:junit-bom:5.9.2"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.assertj:assertj-core:3.24.2")
  testImplementation("com.google.code.gson:gson:2.10.1")
}

tasks.test {
  useJUnitPlatform()
}
