package kraii.util

/**
 * A resource whose [close] method always throws.
 */
class FailingClose : AutoCloseable {
  override fun close() = throw RuntimeException("close failed")
}
