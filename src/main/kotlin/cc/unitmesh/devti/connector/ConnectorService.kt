package cc.unitmesh.devti.connector

import cc.unitmesh.devti.connector.custom.CustomConnector
import cc.unitmesh.devti.connector.openai.OpenAIConnector
import cc.unitmesh.devti.settings.DEFAULT_AI_ENGINE
import cc.unitmesh.devti.settings.DevtiSettingsState
import com.intellij.openapi.application.ApplicationManager

class ConnectorService {
    private val aiEngine: String = DevtiSettingsState.getInstance()?.aiEngine ?: DEFAULT_AI_ENGINE
    fun connector(): CodeCopilot {
        return when (aiEngine) {
            "OpenAI" -> OpenAIConnector()
            "Custom" -> CustomConnector()
            else -> OpenAIConnector()
        }
    }

    companion object {
        fun getInstance(): ConnectorService {
            return ApplicationManager.getApplication().getService(ConnectorService::class.java)
        }
    }
}