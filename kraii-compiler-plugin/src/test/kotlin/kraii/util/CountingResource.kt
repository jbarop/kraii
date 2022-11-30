package kraii.util

import com.google.gson.Gson

/**
 * A stub resource which tracks initializations and closings.
 */
class CountingResource(
  private val name: String,
) : AutoCloseable {

  data class Status(
    val initialized: MutableList<String> = mutableListOf(),
    val closed: MutableList<String> = mutableListOf(),
  )

  companion object {

    private val gson = Gson()

    private val status = Status()

    fun serialize(): String = "CountingResource=${gson.toJson(status)}\n"

    fun deserialize(lines: List<String>): Status {
      val line = lines.find { it.startsWith("CountingResource=") }
        ?: error("'CountingResource' not found in output.")
      val split = line.split("=")
      if (split.size != 2) error("Cannot parse '$line'.")
      return gson.fromJson(split[1], Status::class.java)
    }
  }

  init {
    status.initialized += name
    println("CountingResource(\"$name\") initialized")
  }

  override fun close() {
    status.closed += name
    println("CountingResource(\"$name\") closed")
  }

}
