package cn.devecor.fastbuild.scanner

class FakeFile(
  private val text: String,
  override val name: String
) : VirtualFile {
  override fun findFileOrDirectory(relativePath: String): VirtualFile? {
    return null
  }

  override fun readText(): String {
    return text
  }

  override val isFile: Boolean
    get() = true
}
