package com.github.badfalcon.backlog.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil

@Service
@State(
    name = "com.github.badfalcon.backlog.MyPluginSettingsState",
    storages = [Storage("MyPluginSettings.xml")])
class MyPluginSettingsState : PersistentStateComponent<MyPluginSettingsState> {
    var workspaceName: String = ""
    var apiKey: String = ""

    companion object {
        fun getInstance(): MyPluginSettingsState {
            return ApplicationManager.getApplication().getService(MyPluginSettingsState::class.java)
        }
    }

    override fun getState(): MyPluginSettingsState {
        return this
    }

    override fun loadState(state: MyPluginSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }
}