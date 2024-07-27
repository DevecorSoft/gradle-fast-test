package cn.devecor.fastbuild.scanner

import kotlin.text.trimIndent

class FakeDirHasFile(override val name: String) : VirtualFile {
  override fun findFileOrDirectory(relativePath: String): VirtualFile? {
    if (relativePath == "main/kotlin/cn/devecor/gradlenecessarybuild") {
      return FakeDirHasNoFile("gradlenecessarybuild")
    }

    if (relativePath == "main/kotlin/cn/devecor/gradlenecessarybuild/SupportedLang.kt") {
      return FakeFile(
        """
        package cn.devecor.gradlenecessarybuild
        
        import cn.devecor.gradlenecessarybuild.someThing

        enum class SupportedLang(val ext: String) {
          kotlin("kt"), java("java")
        }
      """.trimIndent(),
        "SupportedLang.kt"
      )
    }

    if (relativePath == "main/kotlin/cn/devecor/gradlenecessarybuild/scanner/Dst.kt") {
      return FakeFile(
        """
        package cn.devecor.gradlenecessarybuild
        
        import cn.devecor.gradlenecessarybuild.someThing

        enum class SupportedLang(val ext: String) {
          kotlin("kt"), java("java")
        }
      """.trimIndent(),
        "Dst.kt"
      )
    }

    if (relativePath == "main/kotlin/cn/devecor/gradlenecessarybuild/someThing") {
      return FakeDirHasNoFile("someThing")
    }
    return null
  }

  override fun readText(): String {
    TODO("Not yet implemented")
  }

  override val isFile: Boolean
    get() = false
}
