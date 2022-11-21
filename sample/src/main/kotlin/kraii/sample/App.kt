package kraii.sample

import kotlin.io.path.createTempFile
import kotlin.io.path.deleteExisting

class ExternalResource : AutoCloseable {

  private val tempFile = createTempFile()

  init {
    println("$tempFile has been created.")
  }

  override fun close(){
    tempFile.deleteExisting()
    println("$tempFile has been deleted.")
  }
}

class ResourceManager : AutoCloseable {
  private val firstResource = ExternalResource()
  private val secondResource = ExternalResource()
}


fun main() {
  ResourceManager().use {
    println("Hello world from Source Code!")
  }
}
