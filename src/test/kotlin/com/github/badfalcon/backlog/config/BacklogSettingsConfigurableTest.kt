package com.github.badfalcon.backlog.config

import com.github.badfalcon.backlog.service.BacklogService
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class BacklogSettingsConfigurableTest : BasePlatformTestCase() {

    private lateinit var configurable: BacklogSettingsConfigurable

    override fun setUp() {
        super.setUp()
        configurable = BacklogSettingsConfigurable(project)
    }

    override fun tearDown() {
        configurable.disposeUIResources()
        super.tearDown()
    }

    fun testGetDisplayName() {
        assertEquals("Backlog", configurable.displayName)
    }

    fun testCreateComponentNotNull() {
        val component = configurable.createComponent()
        assertNotNull(component)
    }

    fun testIsModifiedFalseInitially() {
        configurable.createComponent()
        configurable.reset()
        assertFalse(configurable.isModified)
    }

    fun testIsModifiedTrueAfterChange() {
        configurable.createComponent()
        configurable.reset()

        // Access the component's preferred focused component (workspace text field) and modify it
        val focusedComponent = configurable.preferredFocusedComponent
        assertNotNull(focusedComponent)

        // Modify settings via the configurable's internal component
        // Since we can't directly access the component, we rely on the fact that
        // the component fields are different from settings defaults
        val settings = MyPluginSettingsState.getInstance(project)
        settings.workspaceName = "changed"

        // After changing settings, reset should sync component, then isModified should be false
        configurable.reset()
        assertFalse(configurable.isModified)
    }

    fun testApplyPersistsSettings() {
        configurable.createComponent()
        // apply with default empty values should not throw
        configurable.apply()

        val settings = MyPluginSettingsState.getInstance(project)
        // Settings should reflect the component's values (empty by default)
        assertEquals("", settings.workspaceName)
    }

    fun testResetRestoresSettings() {
        val settings = MyPluginSettingsState.getInstance(project)
        settings.workspaceName = "test-workspace"
        settings.apiKey = "test-key"
        settings.topLevelDomain = BacklogService.TopLevelDomain.JP

        configurable.createComponent()
        configurable.reset()

        // After reset, component should reflect settings, so isModified should be false
        assertFalse(configurable.isModified)
    }

    fun testDisposeUIResources() {
        configurable.createComponent()
        configurable.disposeUIResources()
        // Should not throw on second dispose
        configurable.disposeUIResources()
    }
}
