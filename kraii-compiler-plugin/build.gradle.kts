plugins {
  kotlin("jvm")
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
  implementation(project(":kraii-api"))

  testImplementation(platform("org.junit:junit-bom:5.9.1"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.assertj:assertj-core:3.23.1")
  testImplementation("com.google.code.gson:gson:2.10")
}

tasks.test {
  useJUnitPlatform()
}
