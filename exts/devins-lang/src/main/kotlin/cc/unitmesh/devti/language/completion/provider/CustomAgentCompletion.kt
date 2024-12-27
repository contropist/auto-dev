package cc.unitmesh.devti.language.completion.provider

import cc.unitmesh.devti.agent.model.CustomAgentConfig
import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

class CustomAgentCompletion : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val configs: List<CustomAgentConfig> = CustomAgentConfig.loadFromProject(parameters.originalFile.project)
        configs.forEach { config ->
            result.addElement(LookupElementBuilder.create(config.name)
                .withInsertHandler { context, _ ->
                    context.editor.caretModel.moveCaretRelatively(
                        1 + config.name.length, 0, false, false, false
                    )

                    val toolWindow = AutoDevToolWindowFactory.getToolWindow(context.project)
                    toolWindow?.contentManager?.contents?.map { it.component }?.forEach {
                        if (it is ChatCodingPanel) {
                            it.selectAgent(config)
                        }
                    }
                }
                .withTypeText(config.description, true))
        }
    }
}
