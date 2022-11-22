rootProject.name = "sample"

includeBuild("../kraii-gradle-plugin")
includeBuild("../kraii-compiler-plugin") {
  dependencySubstitution {
    substitute(module("kraii:kraii-compiler-plugin")).using(project(":"))
  }
}
