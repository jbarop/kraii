package kraii.sample.glfw

import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryUtil.NULL

class GlfwContext : AutoCloseable {

  init {
    glfwInit()
    println("GLFW initialized.")
  }

  fun windowHint(hint: Int, value: Int) =
    glfwWindowHint(hint, value)

  fun createWindow(width: Int, height: Int, title: String) =
    glfwCreateWindow(width, height, title, NULL, NULL)

  fun destroyWindow(handle: Long) =
    glfwDestroyWindow(handle)

  fun makeContextCurrent(handle: Long) =
    glfwMakeContextCurrent(handle)

  fun showWindow(handle: Long) =
    glfwShowWindow(handle)

  fun pollEvents() =
    glfwPollEvents()

  fun windowShouldClose(handle: Long) =
    glfwWindowShouldClose(handle)

  fun setKeyCallback(
    handle: Long,
    callback: (window: Long, key: Int, scancode: Int, action: Int, mods: Int) -> Unit,
  ) =
    glfwSetKeyCallback(handle, callback)

  fun freeCallbacks(handle: Long) =
    glfwFreeCallbacks(handle)

  fun swapBuffers(handle: Long) =
    glfwSwapBuffers(handle)

  override fun close() {
    println("GLFW terminated.")
    glfwTerminate()
  }

}
