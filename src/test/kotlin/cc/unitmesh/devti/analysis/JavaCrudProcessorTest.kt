package cc.unitmesh.devti.analysis

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElementFactory
import com.intellij.testFramework.LightPlatformTestCase
import junit.framework.TestCase

class JavaCrudProcessorTest : LightPlatformTestCase() {
    private val javaFactory: PsiElementFactory get() = JavaPsiFacade.getElementFactory(project)

    fun testShould_create_a_new_controller() {
        val originCode = """""".trimIndent()
        val psiClass = javaFactory.createClassFromText(originCode, null)
        psiClass.setName("HelloController")
        psiClass.addBefore(javaFactory.createAnnotationFromText("@Controller", null), psiClass.firstChild)

        TestCase.assertEquals(psiClass.text, """@Controller class HelloController {

}""")
    }

    fun testShould_return_true_when_its_a_service() {
        val processor = JavaCrudProcessor(project)

        val originCode = """@Service class HelloService {
}
            """

        TestCase.assertTrue(processor.isService(originCode))
    }
}