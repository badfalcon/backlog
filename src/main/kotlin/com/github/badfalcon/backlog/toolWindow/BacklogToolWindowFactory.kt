package com.github.badfalcon.backlog.toolWindow

import com.github.badfalcon.backlog.service.ToolWindowService
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class BacklogToolWindowFactory : ToolWindowFactory {
    init {
        thisLogger().info("[backlog] "+ "BacklogToolWindowFactory.init")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        thisLogger().info("[backlog] "+ "createToolWindowContent")
        // initialize tool window service
        val service = project.service<ToolWindowService>()
    }

    override fun shouldBeAvailable(project: Project) = true
}
