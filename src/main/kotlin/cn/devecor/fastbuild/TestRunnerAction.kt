package cn.devecor.fastbuild

import cn.devecor.fastbuild.scanner.filterFirstNotNull
import cn.devecor.fastbuild.scanner.resolveGroup
import cn.devecor.fastbuild.scanner.scanSourceFiles
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.findOrCreateDirectory
import com.intellij.openapi.vfs.findOrCreateFile
import com.intellij.openapi.vfs.readText
import com.intellij.openapi.vfs.writeText
import com.intellij.util.containers.toArray
import org.jetbrains.plugins.gradle.execution.build.CachedModuleDataFinder
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.jetbrains.plugins.gradle.util.cmd.node.GradleCommandLine
import java.util.Optional
import kotlin.io.path.Path
import kotlin.io.path.pathString

const val taskStatement = """tasks.register("fastClasspath")"""

class TestRunnerAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
    if (!file.path.contains("src/test")) return

    val gradleModuleData = CachedModuleDataFinder.getGradleModuleData(
      ProjectFileIndex.getInstance(project).getModuleForFile(file) ?: return
    ) ?: return
    val moduleRootDirPath = gradleModuleData.gradleProjectDir

    val srcDirVirtualFile = VirtualFileManager.getInstance()
      .findFileByUrl("file://$moduleRootDirPath/src") ?: return
    val lang = SupportedLang.of(file.extension!!) ?: return
    val group = resolveGroup(lang, VirtualFileAdapter(file))
    val sourceFiles = scanSourceFiles(VirtualFileAdapter(srcDirVirtualFile), VirtualFileAdapter(file), lang, group)

    val destinationRelativeTestFilePath = Path(moduleRootDirPath).relativize(Path(file.path)).pathString

    val javacPath = resolveJavacPath().get()
    val classpath = resolveClasspath(project, moduleRootDirPath).get()

    fastCompileJava(
      javacPath,
      classpath,
      sourceFiles.toArray(emptyArray()) + arrayOf(destinationRelativeTestFilePath)
    )

    executeTest(project, file.name)
  }

  private fun resolveJavacPath(): Optional<String> {
    val sdk = ProjectJdkTable.getInstance().allJdks.firstOrNull {
      it.sdkType.name == "JavaSDK"
    } ?: return Optional.empty()
    return Optional.of(sdk.homePath + "/bin/javac")
  }

  private fun resolveBuildScriptFile(moduleRootDirPath: String): Optional<VirtualFile> {
    val buildScriptFile = ScriptType.values().map {
      VirtualFileManager.getInstance().findFileByUrl("file://$moduleRootDirPath/${it.fileName}") to it
    }
      .filterFirstNotNull()
      .firstOrNull()

    return Optional.ofNullable(buildScriptFile?.first)
  }

  private fun resolveClasspath(project: Project, moduleRootDirPath: String): Optional<String> {
    val classpathRelativePath = "build/fast-test/classpath"
    val taskName = "fastClasspath"
    val runManager = RunManager.getInstance(project)
    val settings = runManager.createConfiguration(
      taskName,
      GradleExternalTaskConfigurationType::class.java
    )

    val configuration = settings.configuration
    (configuration as GradleRunConfiguration).apply {
      this.name = taskName
      this.isRunAsTest = false
      this.commandLine = GradleCommandLine.parse(
        listOf(
          "fastClasspath"
        )
      )
      this.settings.externalProjectPath = project.basePath
    }

    runManager.addConfiguration(settings)
    runManager.setTemporaryConfiguration(settings)

    val executor = DefaultRunExecutor.getRunExecutorInstance()
    val runnerAndConfigurationSettings = runManager.findSettings(configuration) ?: return Optional.empty()

    val fastClasspathConfig = fastClasspathConfig(classpathRelativePath)

    val buildScriptVirtualFile = resolveBuildScriptFile(moduleRootDirPath).get()
    val originalBuildScriptContent = buildScriptVirtualFile.readText()
    if (!originalBuildScriptContent.contains(taskStatement)) {
      WriteCommandAction.runWriteCommandAction(project) {
        buildScriptVirtualFile.writeText(originalBuildScriptContent + fastClasspathConfig)
      }
    }

    ProgramRunnerUtil.executeConfiguration(runnerAndConfigurationSettings, executor)

    return Optional.of(
      VirtualFileManager.getInstance()
        .findFileByUrl("file://$moduleRootDirPath/$classpathRelativePath")!!
        .readText()
        .trim()
        .removeSurrounding("\r\n")
        .removeSurrounding("\n")
    )
  }

  private fun executeTest(project: Project, taskName: String) {
    val runManager = RunManager.getInstance(project)
    val settings = runManager.createConfiguration(
      taskName,
      GradleExternalTaskConfigurationType::class.java
    )

    val configuration = settings.configuration
    (configuration as GradleRunConfiguration).apply {
      this.name = taskName
      this.isRunAsTest = false
      this.commandLine = GradleCommandLine.parse(
        listOf(
          "test",
          "-x",
          "compileJava",
          "-x",
          "compileTestJava"
        )
      )
      this.settings.externalProjectPath = project.basePath
    }

    runManager.addConfiguration(settings)
    runManager.setTemporaryConfiguration(settings)

    val executor = DefaultRunExecutor.getRunExecutorInstance()
    val runnerAndConfigurationSettings = runManager.findSettings(configuration) ?: return
    ProgramRunnerUtil.executeConfiguration(runnerAndConfigurationSettings, executor)
  }
}

fun srcDirsConfig(scriptType: ScriptType, dirs: List<String>): String {
  if (dirs.isEmpty()) return ""
  return when (scriptType) {
    ScriptType.KOTLIN -> """setSrcDirs(listOf(${dirs.joinToString(", ") { "\"$it\"" }}))"""
    ScriptType.GROOVY -> """srcDirs = [${dirs.joinToString(", ") { "'$it'" }}]"""
  }
}

fun fastClasspathConfig(classpathRelativePath: String): String {
  return """
$taskStatement {
  doFirst {
    project.file("$classpathRelativePath").writeText(project.sourceSets["test"].runtimeClasspath.asPath + project.sourceSets["main"].runtimeClasspath.asPath + project.sourceSets["main"].compileClasspath.asPath)
  }
}"""
}

fun fastCompileJava(
  javacAbsolutePath: String,
  classPath: String,
  sourceFiles: Array<String>,
) {
  try {
    ProcessBuilder(
      javacAbsolutePath,
      "-cp",
      classPath,
      "-d",
      "build/classes/java/test",
      *sourceFiles
    )
      .inheritIO()
      .start()
  } catch (e: Throwable) {
    e.printStackTrace()
  }
}

fun VirtualFile.copyRecursivelyTo(dst: VirtualFile) {
  if (isDirectory) {
    val newDir = dst.findOrCreateDirectory(name)
    children.forEach { it.copyRecursivelyTo(newDir) }
  } else {
    val newFile = dst.findOrCreateFile(name)
    newFile.writeText(readText())
  }
}
