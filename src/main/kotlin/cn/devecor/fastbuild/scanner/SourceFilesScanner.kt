package cn.devecor.fastbuild.scanner

import cn.devecor.fastbuild.SupportedLang

interface VirtualFile {
  fun findFileOrDirectory(relativePath: String): VirtualFile?
  fun readText(): String
  val isFile: Boolean
  val name: String
  val children: Array<VirtualFile>
    get() = emptyArray()
}

fun scanSourceFiles(
  srcDir: VirtualFile,
  dst: VirtualFile,
  lang: SupportedLang,
  group: String,
  initDirs: List<String> = emptyList()
): List<String> {
  val candidates = (fullNames(lang, dst) + selfMainFullNames(lang, dst))
    .filter { it.startsWith(group) }
    .distinct()
//    .map { if (it.endsWith(".*")) it.dropLast(2) else it }
    .map { it.split(".").joinToString("/") }
    .flatMap { listOf("main/$lang/$it", "test/$lang/$it") }
    .flatMap { listOf(it, "$it.${lang.ext}") }
    .filter { !initDirs.contains(it) }
    .map { it to srcDir.findFileOrDirectory(it) }
    .filterSecondNotNull()

  return candidates
    .flatMap {
      if (it.second.isFile) return@flatMap scanSourceFiles(
        srcDir,
        it.second,
        lang,
        group,
        (candidates.map { it.first } + initDirs).distinct()
      ) + listOf(it.first)
      return@flatMap listOf()
    }
    .distinct()
}


val regexps = mapOf(
  SupportedLang.java to FullNameRegex(
    Regex("^package [a-zA-Z.]+;$", RegexOption.MULTILINE),
    Regex("^import [a-zA-Z.]+;$", RegexOption.MULTILINE)
  ),
  SupportedLang.kotlin to FullNameRegex(
    Regex("^package [a-zA-Z.]+$", RegexOption.MULTILINE),
    Regex("^import [a-zA-Z.]+$", RegexOption.MULTILINE)
  )
)

data class FullNameRegex(
  val `package`: Regex,
  val `import`: Regex
)

fun fullNames(supportedLang: SupportedLang, dst: VirtualFile): List<String> {
  return fullNames(supportedLang, dst, regexps[supportedLang]!!.`import`)
}

fun fullNames(supportedLang: SupportedLang, dst: VirtualFile, regex: Regex): List<String> {
  return regex.findAll(dst.readText()).flatMap { it.groups }
    .map { it?.value }
    .filterNotNull()
    .map { it.split(" ").last() }
    .map { if (supportedLang == SupportedLang.java) it.dropLast(1) else it }
    .toList()
}

fun selfMainFullNames(supportedLang: SupportedLang, dst: VirtualFile): List<String> {
  return fullNames(supportedLang, dst, regexps[supportedLang]!!.`package`)
      .map { "$it.${dst.name.split(".")[0].removeSuffix("Test")}" }
    .apply { this.forEach {println(it)} }
}

fun resolveGroup(supportedLang: SupportedLang, dst: VirtualFile): String {
  val packageCode = dst.readText().split("\n").find {
    val candidate = it.trim()
    candidate.startsWith("package") && regexps[supportedLang]!!.`package`.matches(candidate)
  } ?: return ""
  return packageCode.split(" ").last()
    .let { if(it.endsWith(";")) it.dropLast(1) else it }
    .split(".").subList(0, 2).joinToString(".")
}

fun List<String>.normalizeDirs(): List<String> {
  return this.filter { candidate ->
    this.none { it != candidate && candidate.startsWith(it) }
  }
}

fun <A : Any, B : Any> Iterable<Pair<A, B?>>.filterSecondNotNull(): List<Pair<A, B>> {
  return this.fold(mutableListOf<Pair<A, B>>()) { acc, pair ->
    pair.second?.let { acc.add(pair.first to it) }
    acc
  }
}

fun <A : Any, B : Any> Iterable<Pair<A?, B>>.filterFirstNotNull(): List<Pair<A, B>> {
  return this.fold(mutableListOf<Pair<A, B>>()) { acc, pair ->
    pair.first?.let { acc.add(it to pair.second) }
    acc
  }
}
