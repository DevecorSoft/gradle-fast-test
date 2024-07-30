package cn.devecor.fastbuild

import cn.devecor.fastbuild.scanner.filterFirstNotNull
import cn.devecor.fastbuild.scanner.resolveGroup
import cn.devecor.fastbuild.scanner.scanSourceFiles
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.findOrCreateDirectory
import com.intellij.openapi.vfs.findOrCreateFile
import com.intellij.openapi.vfs.readText
import com.intellij.openapi.vfs.writeText
import org.jetbrains.plugins.gradle.execution.build.CachedModuleDataFinder
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.jetbrains.plugins.gradle.util.cmd.node.GradleCommandLine
import kotlin.io.path.Path
import kotlin.io.path.pathString

class TestRunnerAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
    if (!file.path.contains("src/test")) return

    val gradleModuleData = CachedModuleDataFinder.getGradleModuleData(
      ProjectFileIndex.getInstance(project).getModuleForFile(file) ?: return
    ) ?: return

    val gradleProjectDir = gradleModuleData.gradleProjectDir

    val javacPath = resolveJavacPath() ?: return

    val buildScriptFile = ScriptType.values().map {
      VirtualFileManager.getInstance().findFileByUrl("file://$gradleProjectDir/${it.fileName}") to it
    }
      .filterFirstNotNull()
      .firstOrNull() ?: return

    val lang = SupportedLang.of(file.extension!!) ?: return

    val buildScriptVirtualFile = buildScriptFile.first
    val srcDirVirtualFile = VirtualFileManager.getInstance()
      .findFileByUrl("file://$gradleProjectDir/src") ?: return
    val group = resolveGroup(lang, VirtualFileAdapter(file))

    val originalBuildScriptContent = buildScriptVirtualFile.readText()

    val modulePath = Path(gradleProjectDir)

    val sourceFiles = scanSourceFiles(VirtualFileAdapter(srcDirVirtualFile), VirtualFileAdapter(file), lang, group)
    val dstRelativeTestFilePath = modulePath.relativize(Path(file.path)).pathString
    println("ffffffff: ${file.name}")
    val fastTestConfig = fastTestConfig(
      lang,
      testFilesPath = (
        sourceFiles.map { "src/$it" } +
          listOf(dstRelativeTestFilePath)).map { "\"$it\"" }.joinToString(",\n"),
    )

    println(sourceFiles)
    println(fastTestConfig)


    WriteCommandAction.runWriteCommandAction(project) {
      buildScriptVirtualFile.writeText(originalBuildScriptContent + fastTestConfig)
//      buildScriptVirtualFile.parent.parent.findOrCreateDirectory("fast").delete(this)
//      val fastModuleDirVirtualFile = buildScriptVirtualFile.parent.parent.findOrCreateDirectory("fast")
//      sourceDirs.map {
//        srcDirVirtualFile.findDirectory(it)!!
//          .copyRecursivelyTo(fastModuleDirVirtualFile.findOrCreateDirectory(it).parent)
//      }
//      fastModuleDirVirtualFile.findOrCreateFile(buildScriptFile.second.fileName)
//        .writeText(originalBuildScriptContent)
    }


    val runManager = RunManager.getInstance(project)
    val settings = runManager.createConfiguration(
      file.name,
      GradleExternalTaskConfigurationType::class.java
    )

    val configuration = settings.configuration
    (configuration as GradleRunConfiguration).apply {
      this.name = file.name
      this.isRunAsTest = true
      this.commandLine = GradleCommandLine.parse(
        listOf(
          "fastTest",
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
    val environment = ExecutionEnvironmentBuilder.create(executor, runnerAndConfigurationSettings).build {
      it.processHandler?.addProcessListener(object : ProcessAdapter() {
        override fun processTerminated(event: ProcessEvent) {
          println("event: ${event.text}")
        }
      })
    }
    ProgramRunnerUtil.executeConfiguration(environment, true, true)
  }

  private fun resolveJavacPath(): String? {
    val sdk = ProjectJdkTable.getInstance().allJdks.firstOrNull {
      it.sdkType.name == "JavaSDK"
    } ?: return null
    return sdk.homePath + "/bin/javac"
  }
}

fun srcDirsConfig(scriptType: ScriptType, dirs: List<String>): String {
  if (dirs.isEmpty()) return ""
  return when (scriptType) {
    ScriptType.KOTLIN -> """setSrcDirs(listOf(${dirs.joinToString(", ") { "\"$it\"" }}))"""
    ScriptType.GROOVY -> """srcDirs = [${dirs.joinToString(", ") { "'$it'" }}]"""
  }
}

fun fastTestConfig(lang: SupportedLang, testFilesPath: String): String {
  return """
tasks.register("fastClasses") {
  doFirst {
    project.file("build/fast/classpath").writeText(project.sourceSets["test"].runtimeClasspath.asPath + project.sourceSets["main"].runtimeClasspath.asPath + project.sourceSets["main"].compileClasspath.asPath)
  }
}
tasks.register("fastCompile") {
    doFirst {
      exec {
        commandLine = listOf(
          "javac",
          "-cp",
          project.sourceSets["test"].runtimeClasspath.asPath + project.sourceSets["main"].runtimeClasspath.asPath + project.sourceSets["main"].compileClasspath.asPath,
          "-d",
          "build/classes/java/test",
          $testFilesPath
        )
      }
    }                                                                                                                                                                                                                                                                                                          
}
tasks.register("fastTest", Test::class) {
  dependsOn("fastCompile")
}
"""
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
