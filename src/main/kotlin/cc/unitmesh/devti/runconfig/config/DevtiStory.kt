package cc.unitmesh.devti.runconfig.config

import cc.unitmesh.devti.language.StoryConfig
import cc.unitmesh.devti.runconfig.command.BaseConfig

class DevtiStory(
    val storyId: Int,
    private val storySource: String,
    private val acs: List<String> = listOf()
) : BaseConfig() {
    override var configurationName = "DevTi Create Story"

    init {
        if (storyId <= 0) {
            throw IllegalArgumentException("Story id must be greater than 0")
        }

        // update configure name by story id
        configurationName += " $storyId"
    }

    companion object {
        fun fromStoryConfig(storyConfig: StoryConfig): DevtiStory {
            return DevtiStory(storyConfig.storyId, storyConfig.storySource, storyConfig.acs)
        }
    }

    override fun toString(): String {
        return "DevtiCreateStoryConfigure(storyId=$storyId, storySource='$storySource', acs=$acs)"
    }
}