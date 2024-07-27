package cn.devecor.fastbuild

enum class ScriptType(val fileName: String) {
  KOTLIN("build.gradle.kts"), GROOVY("build.gradle")
}
