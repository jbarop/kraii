package kraii

import org.gradle.api.Project
import org.gradle.api.Plugin

class KraiiPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.tasks.register("greeting") { task ->
      task.doLast {
        println("Hello from plugin 'kraii.greeting'")
      }
    }
  }
}
