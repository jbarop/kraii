package kraii.util

/**
 * A resource whose constructor always throws.
 */
class FailingResource : AutoCloseable {
  init {
    throw RuntimeException("constructor failed")
  }

  override fun close() {}
}
