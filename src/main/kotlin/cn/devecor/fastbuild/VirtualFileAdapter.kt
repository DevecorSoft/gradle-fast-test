package cn.devecor.fastbuild

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFileOrDirectory
import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.vfs.readText

typealias ScannerVirtualFile = cn.devecor.fastbuild.scanner.VirtualFile

class VirtualFileAdapter(val virtualFile: VirtualFile): ScannerVirtualFile {
  override fun findFileOrDirectory(relativePath: String): ScannerVirtualFile? {
    val mayBeFile = virtualFile.findFileOrDirectory(relativePath)
    return mayBeFile?.let { VirtualFileAdapter(mayBeFile) }
  }

  override fun readText(): String {
    return virtualFile.readText()
  }

  override val isFile: Boolean
    get() = virtualFile.isFile

  override val name: String
    get() = virtualFile.name

  override val children: Array<ScannerVirtualFile>
    get() = virtualFile.children.map { VirtualFileAdapter(it) }.toTypedArray()
}
