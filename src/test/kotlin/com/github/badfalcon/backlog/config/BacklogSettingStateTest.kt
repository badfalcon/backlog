package com.github.badfalcon.backlog.config

import com.github.badfalcon.backlog.service.BacklogService
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class BacklogSettingStateTest : BasePlatformTestCase() {

    fun testDefaultValues() {
        val state = MyPluginSettingsState.getInstance(project)
        assertEquals("", state.workspaceName)
        assertEquals("", state.apiKey)
        assertEquals("", state.projectName)
        assertEquals(BacklogService.TopLevelDomain.COM, state.topLevelDomain)
    }

    fun testGetStateReturnsSelf() {
        val state = MyPluginSettingsState.getInstance(project)
        assertSame(state, state.getState())
    }

    fun testLoadStateCopiesValues() {
        val state = MyPluginSettingsState.getInstance(project)

        val source = MyPluginSettingsState()
        source.workspaceName = "test-workspace"
        source.apiKey = "test-api-key"
        source.projectName = "test-project"
        source.topLevelDomain = BacklogService.TopLevelDomain.JP

        state.loadState(source)

        assertEquals("test-workspace", state.workspaceName)
        assertEquals("test-api-key", state.apiKey)
        assertEquals("test-project", state.projectName)
        assertEquals(BacklogService.TopLevelDomain.JP, state.topLevelDomain)
    }

    fun testGetInstance() {
        val state1 = MyPluginSettingsState.getInstance(project)
        val state2 = MyPluginSettingsState.getInstance(project)
        assertSame(state1, state2)
    }
}
