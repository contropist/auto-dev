package cc.unitmesh.devti.analysis

import com.intellij.ide.fileTemplates.FileTemplateDescriptor
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import java.util.*

class JavaCrudTemplate(val project: Project) {
    private val fileTemplateManager = FileTemplateManager.getDefaultInstance()
    val controllerTemplate = FileTemplateDescriptor("Java Controller.java", JavaFileType.INSTANCE.icon)

    fun controller(
        controllerName: String,
        code: String,
        packageName: String?
    ): String {
        val template = fileTemplateManager.getCodeTemplate(controllerTemplate.fileName)

        val properties = Properties()
        properties.setProperty("packageName", packageName)
        properties.setProperty("controllerName", controllerName)
        properties.setProperty("code", code)

        return template.getText(properties)
    }

    fun service(serviceName: String, code: String, packageName: String): String {
        val template = fileTemplateManager.getCodeTemplate("Java Service.java")

        val properties = Properties()
        properties.setProperty("packageName", packageName)
        properties.setProperty("serviceName", serviceName)
        properties.setProperty("code", code)

        return template.getText(properties)
    }
}