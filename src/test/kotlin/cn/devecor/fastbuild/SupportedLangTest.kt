package cn.devecor.fastbuild.scanner

import cn.devecor.fastbuild.SupportedLang
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SupportedLangTest {

  @Test
  fun testOfKt() {
    val lang = SupportedLang.of("kt")

    lang shouldBe SupportedLang.kotlin
  }

  @Test
  fun testOfJava() {
    val lang = SupportedLang.of("java")

    lang shouldBe SupportedLang.java
  }

  @Test
  fun testOfElse() {
    val lang = SupportedLang.of("javax")

    lang shouldBe null
  }
}
