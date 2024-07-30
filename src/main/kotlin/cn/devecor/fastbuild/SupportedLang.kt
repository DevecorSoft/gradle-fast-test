package cn.devecor.fastbuild

enum class SupportedLang(val ext: String) {
  kotlin("kt"), java("java");

  companion object {
    fun of(fileExtension: String): SupportedLang? {
      return values().find { it.ext == fileExtension }
    }
  }
}
