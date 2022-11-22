package kraii.sample

import kraii.sample.glfw.GlfwContext
import kraii.sample.glfw.GlfwWindow

class LwjglSample : AutoCloseable {

  private val glfw = GlfwContext()
  private val window = GlfwWindow(glfw, 800, 600, "kraii-lwjgl-sample")

  fun loop() {
    window.onKeyPress { println("Key $it was pressed.") }
    window.loop {
      //
    }
  }

}

fun main() {
  LwjglSample().use(LwjglSample::loop)
}
