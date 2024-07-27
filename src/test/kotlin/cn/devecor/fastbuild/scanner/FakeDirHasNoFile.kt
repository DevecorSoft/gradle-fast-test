package cn.devecor.fastbuild.scanner

class FakeDirHasNoFile(
  override val name: String
) : VirtualFile {
  override fun findFileOrDirectory(relativePath: String): VirtualFile? {
    if (relativePath == "main/kotlin/cn/devecor/gradlenecessarybuild") {
      return FakeDirHasNoFile("gradlenecessarybuild")
    }
    return null
  }

  override fun readText(): String {
    TODO("Not yet implemented")
  }

  override val isFile: Boolean
    get() = false
}
