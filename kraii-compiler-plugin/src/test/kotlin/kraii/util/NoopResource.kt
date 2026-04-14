package kraii.util

/**
 * A resource whose [close] method does nothing.
 */
class NoopResource : AutoCloseable {
  override fun close() {}
}
