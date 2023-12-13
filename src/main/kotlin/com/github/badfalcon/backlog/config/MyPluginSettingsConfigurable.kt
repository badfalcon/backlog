package com.github.badfalcon.backlog.config


import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.Nullable
import javax.swing.JComponent


class MyPluginSettingsConfigurable : Configurable {
    private var mySettingsComponent: MyPluginSettingsComponent? = MyPluginSettingsComponent()

    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getDisplayName(): String {
        return "Backlog"
    }

    @Nullable
    override fun getPreferredFocusedComponent(): JComponent? {
        return mySettingsComponent?.preferredFocusedComponent
    }

    @Nullable
    override fun createComponent(): JComponent? {
        mySettingsComponent = MyPluginSettingsComponent()
        return mySettingsComponent?.panel
    }

    override fun isModified(): Boolean {
        val settings: MyPluginSettingsState = MyPluginSettingsState.getInstance()
        var modified: Boolean = (mySettingsComponent?.workspaceNameText != settings.workspaceName)
        modified = modified or (mySettingsComponent?.apiKeyText != settings.apiKey)
        return modified
    }

    override fun apply() {
        val settings: MyPluginSettingsState = MyPluginSettingsState.getInstance()
        settings.workspaceName = mySettingsComponent?.workspaceNameText!!
        settings.apiKey = mySettingsComponent?.apiKeyText!!
        println("https://${settings.workspaceName}.backlog.jp/api/v2/users/myself?apiKey=${settings.apiKey}")
    }

    override fun reset() {
        val settings: MyPluginSettingsState = MyPluginSettingsState.getInstance()
        mySettingsComponent?.workspaceNameText = settings.workspaceName
        mySettingsComponent?.apiKeyText = settings.apiKey
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}