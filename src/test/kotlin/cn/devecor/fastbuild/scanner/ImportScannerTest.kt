package cn.devecor.fastbuild.scanner

import cn.devecor.fastbuild.SupportedLang
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ImportScannerTest {

  @Nested
  inner class ScanSourceDirs {

    @Test
    fun `should return paths of current file`() {
      val srcDir = FakeDirHasNoFile("src")
      val dst = FakeFile(
        """
      package cn.devecor.gradlenecessarybuild.scanner

      import cn.devecor.gradlenecessarybuild.SupportedLang
      import io.kotest.matchers.shouldBe
      import org.junit.jupiter.api.Test
    """.trimIndent(),
        "DstTest.kt"
      )

      val dirs = scanSourceDirs(srcDir, dst, SupportedLang.kotlin, "cn.devecor")

      dirs shouldContain "main/kotlin/cn/devecor/gradlenecessarybuild"
    }

    @Test
    fun `should return paths of deps file`() {
      val srcDir = FakeDirHasFile("src")
      val dst = FakeFile(
        """
      package cn.devecor.gradlenecessarybuild.scanner

      import cn.devecor.gradlenecessarybuild.SupportedLang
      import io.kotest.matchers.shouldBe
      import org.junit.jupiter.api.Test
    """.trimIndent(),
        "DstTest.kt"
      )

      val dirs = scanSourceDirs(srcDir, dst, SupportedLang.kotlin, "cn.devecor")

      dirs shouldContain "main/kotlin/cn/devecor/gradlenecessarybuild/someThing"
      dirs shouldContain "main/kotlin/cn/devecor/gradlenecessarybuild"
    }

    @Test
    fun `should remove child dirs`() {
      val dirs = listOf(
        "main/kotlin/cn/devecor/gradlenecessarybuild",
        "main/kotlin/cn/devecor/gradlenecessarybuild/sub1",
        "main/kotlin/cn/devecor/gradlenecessarybuild/sub2",
        "main/kotlin/cn/devecor/siblings",
      ).normalizeDirs()

      dirs shouldHaveSize 2
      dirs shouldContain "main/kotlin/cn/devecor/gradlenecessarybuild"
      dirs shouldContain "main/kotlin/cn/devecor/siblings"
    }

    @Nested
    inner class ScanSourceFiles {
      @Test
      fun `should return paths of deps file`() {
        val srcDir = FakeDirHasFile("src")
        val dst = FakeFile(
          """
      package cn.devecor.gradlenecessarybuild.scanner

      import cn.devecor.gradlenecessarybuild.SupportedLang
      import io.kotest.matchers.shouldBe
      import org.junit.jupiter.api.Test
      
      class DstTest {
        @Test
        fun test() {
          Dst()
        }
      }
    """.trimIndent(),
          "DstTest.kt"
        )

        val dirs = scanSourceFiles(srcDir, dst, SupportedLang.kotlin, "cn.devecor")

        dirs shouldContain "main/kotlin/cn/devecor/gradlenecessarybuild/SupportedLang.kt"
        dirs shouldContain "main/kotlin/cn/devecor/gradlenecessarybuild/scanner/Dst.kt"
      }
    }
  }
}