package com.github.badfalcon.backlog.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.github.badfalcon.backlog.MyBundle
import com.github.badfalcon.backlog.config.MyPluginSettingsState
import com.github.badfalcon.backlog.toolWindow.MyToolWindowFactory
import com.intellij.dvcs.repo.Repository
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import com.nulabinc.backlog4j.BacklogClient
import com.nulabinc.backlog4j.BacklogClientFactory
import com.nulabinc.backlog4j.PullRequest
import com.nulabinc.backlog4j.api.option.PullRequestQueryParams
import com.nulabinc.backlog4j.conf.BacklogConfigure
import com.nulabinc.backlog4j.conf.BacklogJpConfigure
import git4idea.GitUtil
import git4idea.changes.GitChangeUtils
import git4idea.fetch.GitFetchSupport
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener

/*
@Service(Service.Level.PROJECT)
class MyProjectService(private var project: Project) {
    var toolWindowManager : ToolWindowManager? = null
    var myToolWindow: MyToolWindowFactory.MyToolWindow? = null

    init {
        thisLogger().warn("[BLPL] Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")

        toolWindowManager = ToolWindowManager.getInstance(project)
    }

    fun requestToolWindowUpdate()
    {
        thisLogger().debug("[BLPL]requestToolWindowUpdate")

        val backlogService = service<BacklogService>()
        val gitService = service<GitService>()
        if (backlogService.isReady && gitService.isReady){
            val openProject = ProjectManager.getInstance().openProjects.first()
            val fetch = GitFetchSupport.fetchSupport(openProject)
            fetch.fetchAllRemotes(listOf(gitService.repository)).showNotificationIfFailed()
            ApplicationManager.getApplication().invokeLater {
                thisLogger().debug("[BLPL]invokeLater")

                var toolWindow: ToolWindow? = toolWindowManager?.getToolWindow(MyToolWindowFactory.TOOL_WINDOW_ID)
                if(myToolWindow != null){
                    val content = ContentFactory.getInstance().createContent(myToolWindow!!.getContent(), null, false)
                    val contentManager = toolWindow?.contentManager!!
                    contentManager.removeAllContents(true)
                    contentManager.addContent(content)
               }
            }
        }
    }

    fun getRandomNumber() = (1..100).random()
}
*/
