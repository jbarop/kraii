package kraii.sample

import kraii.api.Scoped
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
 * A class which uses other resources.
 *
 * It extends the [AutoCloseable] interface, but does
 * implement [AutoCloseable.close]. This is done
 * automatically by the compiler plugin.
 */
class ResourceOwner : AutoCloseable {

  @Scoped
  private val firstResource = ExternalResource()

  @Scoped
  private val container = mutableListOf<ExternalResource>()

  @Scoped
  private val secondResource = ExternalResource()

  fun createNewFile() {
    container += ExternalResource()
  }
}

fun main(args: Array<String>) {
  @Scoped val container = ResourceOwner()
  repeat(numberOfFilesToBeCreated(args)) {
    container.createNewFile()
  }

  println("Hello World!")
}

/**
 * Determines the number of files to be created.
 *
 * Interactively queries the user or, if specified, uses the command line parameters.
 */
private fun numberOfFilesToBeCreated(args: Array<String>): Int =
  if (args.isEmpty()) {
    print("How many additional files should be created? ")
    readln().toInt()
  } else {
    println("Creating ${args[0].toInt()} additional files.")
    args[0].toInt()
  }
