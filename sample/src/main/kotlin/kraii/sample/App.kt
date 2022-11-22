package kraii.sample

import kotlin.io.path.createTempFile
import kotlin.io.path.deleteExisting

/**
 * A temporary file as an example for a non-heap
 * resource.
 */
class ExternalResource : AutoCloseable {

  private val tempFile = createTempFile()

  init {
    println("$tempFile has been created.")
  }

  /**
   * The implementation of [AutoCloseable.close] is
   * responsible for freeing the resource. In this
   * case the file needs to be removed.
   */
  override fun close() {
    tempFile.deleteExisting()
    println("$tempFile has been deleted.")
  }
}

/**
 * An 'complex' object which uses other resources.
 *
 * It extends the [AutoCloseable] interface, but does
 * implement [AutoCloseable.close]. This is done
 * automatically by the compiler plugin.
 */
class ResourceManager : AutoCloseable {
  private val firstResource = ExternalResource()
  private val secondResource = ExternalResource()
}

fun main() {
  ResourceManager().use {
    println("Hello World!")
  }
}
