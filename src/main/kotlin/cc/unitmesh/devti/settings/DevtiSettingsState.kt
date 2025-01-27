package cc.unitmesh.devti.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "cc.unitmesh.devti.settings.DevtiSettingsState", storages = [Storage("DevtiSettings.xml")])
class DevtiSettingsState : PersistentStateComponent<DevtiSettingsState> {

    var githubToken = ""
    var openAiKey = ""
    var openAiModel = ""

    var aiEngine = DEFAULT_AI_ENGINE
    var customOpenAiHost = ""
    var customEngineServer = ""
    var customEngineToken = ""
    var customEnginePrompts = ""

    override fun getState(): DevtiSettingsState {
        return this
    }

    override fun loadState(state: DevtiSettingsState) {
        if (customEnginePrompts == "") {
            customEnginePrompts = DEFAULT_PROMPTS.trimIndent()
        }

        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): DevtiSettingsState? {
            return ApplicationManager.getApplication().getService(DevtiSettingsState::class.java)
        }
    }

} 
