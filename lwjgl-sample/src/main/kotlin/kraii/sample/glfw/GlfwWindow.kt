package kraii.sample.glfw

import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL.createCapabilities
import org.lwjgl.opengl.GL11.*

class GlfwWindow(
  private val glfw: GlfwContext,
  width: Int,
  height: Int,
  title: String,
) : AutoCloseable {

  private val handle: Long

  init {
    glfw.windowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE)
    glfw.windowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE)
    handle = glfw.createWindow(width, height, title)
    println("Window created.")
  }

  fun onKeyPress(callback: (key: Int) -> Unit) {
    glfw.setKeyCallback(handle) { _: Long, key: Int, _: Int, action: Int, _: Int ->
      if (action == 1) callback(key)
    }
  }

  fun loop(frame: () -> Unit) {
    glfw.makeContextCurrent(handle)
    glfw.showWindow(handle)
    createCapabilities()
    glClearColor(0.3f, 0.3f, 0.3f, 0.0f)
    while (!glfw.windowShouldClose(handle)) {
      glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
      frame()
      glfw.swapBuffers(handle)
      glfw.pollEvents()
    }
  }

  override fun close() {
    glfw.freeCallbacks(handle)
    glfw.destroyWindow(handle)
    println("Window destroyed.")
  }

}
