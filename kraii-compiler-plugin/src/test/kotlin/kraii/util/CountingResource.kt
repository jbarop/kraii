package kraii.util

/**
 * A stub resource which counts how many instances were constructed and closed.
 */
class CountingResource : AutoCloseable {

  companion object {
    var numInitialized = 0
    var numClosed = 0
  }

  init {
    numInitialized++
    println("CountingResource initialized")
  }

  override fun close() {
    numClosed++
    println("CountingResource closed")
  }

}
