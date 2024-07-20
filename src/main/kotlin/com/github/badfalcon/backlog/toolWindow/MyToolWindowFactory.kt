package com.github.badfalcon.backlog.toolWindow

import com.github.badfalcon.backlog.MyBundle
import com.github.badfalcon.backlog.services.ToolWindowService
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class MyToolWindowFactory : ToolWindowFactory {
    init {
        thisLogger().warn("[backlog] "+ "MyToolWindowFactory.init")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        thisLogger().warn("[backlog] "+ "createToolWindowContent")
        println("createToolWindowContent")
        // initialize tool window service
        val service = toolWindow.project.service<ToolWindowService>()
        service.toolWindow = toolWindow

        // create home tab
        val homeTab = service.createHomeTabContent(null)
        val content = ContentFactory.getInstance().createContent(homeTab, MyBundle.message("toolWindowHomeTabTitle"), false)
        content.isCloseable = false
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true
}
