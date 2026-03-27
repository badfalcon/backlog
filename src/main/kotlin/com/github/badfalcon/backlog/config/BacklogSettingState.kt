package com.github.badfalcon.backlog.config

import com.github.badfalcon.backlog.service.BacklogService
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.PROJECT)
@State(
    name = "com.github.badfalcon.backlog.MyPluginSettingsState",
    storages = [Storage("BacklogSettings.xml")])
class MyPluginSettingsState : PersistentStateComponent<MyPluginSettingsState> {
    var workspaceName: String = ""
    var apiKey: String = ""
    var projectName: String = ""
    var topLevelDomain: BacklogService.TopLevelDomain = BacklogService.TopLevelDomain.COM

    companion object {
        fun getInstance(project: Project): MyPluginSettingsState {
            return project.getService(MyPluginSettingsState::class.java)
        }
    }

    override fun getState(): MyPluginSettingsState {
        return this
    }

    override fun loadState(state: MyPluginSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }
}