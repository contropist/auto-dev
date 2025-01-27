package cc.unitmesh.devti.analysis

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiPackageStatement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiTreeUtil


class JavaCrudProcessor(val project: Project) : CrudProcessor {
    private val psiElementFactory = JavaPsiFacade.getElementFactory(project)
    private val codeTemplate = JavaCrudTemplate(project)

    private val controllers = getAllControllerFiles()
    private val services = getAllServiceFiles()

    private fun getAllControllerFiles(): List<PsiFile> {
        val psiManager = PsiManager.getInstance(project)

        val searchScope: GlobalSearchScope = ProjectScope.getContentScope(project)
        val javaFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, searchScope)

        return filterFiles(javaFiles, psiManager, ::controllerFilter)
    }

    private fun getAllServiceFiles(): List<PsiFile> {
        val psiManager = PsiManager.getInstance(project)

        val searchScope: GlobalSearchScope = ProjectScope.getContentScope(project)
        val javaFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, searchScope)

        return filterFiles(javaFiles, psiManager, ::serviceFilter)
    }

    private fun filterFiles(
        javaFiles: Collection<VirtualFile>,
        psiManager: PsiManager,
        filter: (PsiClass) -> Boolean
    ) = javaFiles
        .mapNotNull { virtualFile -> psiManager.findFile(virtualFile) }
        .filter { psiFile ->
            val psiClass = PsiTreeUtil.findChildrenOfType(psiFile, PsiClass::class.java)
                .firstOrNull()
            psiClass != null && filter(psiClass)
        }

    private fun controllerFilter(clazz: PsiClass): Boolean = clazz.annotations
        .map { it.qualifiedName }.any {
            it == "org.springframework.stereotype.Controller" ||
                    it == "org.springframework.web.bind.annotation.RestController"
        }

    private fun serviceFilter(clazz: PsiClass): Boolean = clazz.annotations
        .map { it.qualifiedName }.any {
            it == "org.springframework.stereotype.Service"
        }

    private fun repositoryFilter(clazz: PsiClass): Boolean = clazz.annotations
        .map { it.qualifiedName }.any {
            it == "org.springframework.stereotype.Repository"
        }

    fun addMethodToClass(psiClass: PsiClass, method: String): PsiClass {
        val methodFromText = psiElementFactory.createMethodFromText(method, psiClass)
        var lastMethod: PsiMethod? = null
        val allMethods = psiClass.methods

        if (allMethods.isNotEmpty()) {
            lastMethod = allMethods[allMethods.size - 1]
        }

        if (lastMethod != null) {
            psiClass.addAfter(methodFromText, lastMethod)
        } else {
            psiClass.add(methodFromText)
        }

        return psiClass
    }

    override fun controllerList(): List<DtClass> {
        return this.controllers.map {
            val className = it.name.substring(0, it.name.length - ".java".length)
            DtClass.fromPsiFile(it) ?: DtClass(className, emptyList())
        }
    }

    override fun serviceList(): List<DtClass> {
        TODO("Not yet implemented")
    }

    override fun modelList(): List<DtClass> {
        TODO("Not yet implemented")
    }

    override fun createControllerOrUpdateMethod(targetController: String, code: String, isControllerExist: Boolean) {
        if (!isControllerExist) {
            this.createController(targetController, code)
            return
        }

        val targetControllerFile = controllers.first { it.name == "$targetController.java" }
        val targetControllerClass = PsiTreeUtil.findChildrenOfType(targetControllerFile, PsiClass::class.java)
            .firstOrNull() ?: return

        var method = code
        if (code.contains("class $targetController")) {
            method = code.substring(code.indexOf("{") + 1, code.lastIndexOf("}"))
        }

        method = method.trimIndent()

        WriteCommandAction.writeCommandAction(project)
            .run<RuntimeException> {
                // add method to class
                addMethodToClass(targetControllerClass, method)
                CodeStyleManager.getInstance(project).reformat(targetControllerFile)
            }
    }

    override fun createController(endpoint: String, code: String): DtClass? {
        if (controllers.isEmpty()) {
            return DtClass("", emptyList())
        }

        val randomController = firstController()
        val packageStatement = randomController.lookupPackageName()

        if (packageStatement == null) {
            log.warn("No package statement found in file ${randomController.name}")
            return DtClass("", emptyList())
        }

        val templateCode = codeTemplate.controller(endpoint, code, packageStatement.packageName)

        val parentDirectory = randomController.virtualFile?.parent ?: return null
        val fileSystem = randomController.virtualFile?.fileSystem

        createClassByTemplate(parentDirectory, fileSystem, endpoint, templateCode)

        return DtClass(endpoint, emptyList())
    }

    override fun isService(code: String): Boolean {
        if (code.contains("class ")) {
            return true
        }

        val createClassFromText = psiElementFactory.createClassFromText(code, null)
        return createClassFromText.name?.contains("Service") ?: false
    }

    override fun createService(code: String): DtClass? {
        val firstService = services.first()
        val packageName = if (services.isNotEmpty()) {
            firstService.lookupPackageName()?.packageName
        } else {
            defaultPackageByController()
        }

        if (packageName == null) {
            log.warn("No package statement found in file ${firstService.name}")
            return DtClass("", emptyList())
        }

        return createClass(code, packageName)
    }

    override fun createClass(code: String, packageName: String?): DtClass? {
        val parentDirectory = firstController().virtualFile?.parent ?: return null
        val fileSystem = firstController().virtualFile?.fileSystem

        val newClass = psiElementFactory.createClassFromText(code, null)
        val className = newClass.identifyingElement?.text ?: "DummyClass"

        ApplicationManager.getApplication().invokeLater {
            runWriteAction {
                val virtualFile = parentDirectory.createChildData(fileSystem, "$className.java")
                VfsUtil.saveText(virtualFile, code)

                log.warn("Created file ${virtualFile.path}")
                parentDirectory.refresh(false, true)
            }
        }

        return null
    }

    private fun defaultPackageByController() = firstController().lookupPackageName()?.packageName

    private fun firstController() = controllers.first()

    private fun createClassByTemplate(
        parentDirectory: VirtualFile,
        fileSystem: VirtualFileSystem?,
        serviceName: String,
        templateCode: String
    ) {
        ApplicationManager.getApplication().invokeLater {
            runWriteAction {
                val virtualFile = parentDirectory.createChildData(fileSystem, "$serviceName.java")
                VfsUtil.saveText(virtualFile, templateCode)

                log.warn("Created file ${virtualFile.path}")
                parentDirectory.refresh(false, true)
            }
        }
    }

    companion object {
        private val log: Logger = logger<JavaCrudProcessor>()
    }
}

private fun PsiFile.lookupPackageName(): PsiPackageStatement? {
    val packageStatement = runReadAction {
        PsiTreeUtil.findChildrenOfType(this, PsiPackageStatement::class.java)
            .firstOrNull() ?: return@runReadAction null
    }
    return packageStatement
}
