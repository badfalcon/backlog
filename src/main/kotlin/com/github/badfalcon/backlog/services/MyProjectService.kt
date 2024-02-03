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
import git4idea.fetch.GitFetchSupport
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener

@Service(Service.Level.PROJECT)
class MyProjectService(project: Project) {
    var toolWindowManager : ToolWindowManager? = null
    var myToolWindow: MyToolWindowFactory.MyToolWindow? = null
    var backlogClient : BacklogClient? = null
    var repository : GitRepository? = null

    init {
        thisLogger().info("[BLPL]" + MyBundle.message("projectService", project.name))
        thisLogger().warn("[BLPL] Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")

        toolWindowManager = ToolWindowManager.getInstance(project)

        val settings: MyPluginSettingsState = MyPluginSettingsState.getInstance()

        if(settings.apiKey != "" && settings.workspaceName != ""){
            val configure: BacklogConfigure = BacklogJpConfigure(settings.workspaceName).apiKey(settings.apiKey);
            if(testConfigValues(settings.workspaceName, settings.apiKey)){
                backlogClient = BacklogClientFactory(configure).newClient()
                // todo try repaint
            }
        }

        project.messageBus.connect().subscribe(GitRepository.GIT_REPO_CHANGE, GitRepositoryChangeListener {
            val manager = GitUtil.getRepositoryManager(project)
            val basePath: String? = project.basePath
            if(basePath == null){
                repository = null
                return@GitRepositoryChangeListener
            }

            val rootDirectory: VirtualFile? = LocalFileSystem.getInstance().findFileByPath(basePath);
            if (rootDirectory == null){
                repository = null
                return@GitRepositoryChangeListener
            }

            val repo: GitRepository? = manager.getRepositoryForRoot(rootDirectory)
            if (repo == null){
                repository = null
                return@GitRepositoryChangeListener
            }
            repository = repo
            thisLogger().debug("[BLPL]repository")

            // todo ui 更新
            requestToolWindowUpdate(/*project*/)
        })
    }

    fun requestToolWindowUpdate()
    {
        thisLogger().debug("[BLPL]requestToolWindowUpdate")
        if (backlogClient != null && repository != null){
            val fetch = GitFetchSupport.fetchSupport(repository!!.project)
            val res = fetch.fetchAllRemotes(listOf(repository)).showNotificationIfFailed()
            ApplicationManager.getApplication().invokeLater {
                thisLogger().debug("[BLPL]invokeLater")
                // todo stop hard-coding
                var toolWindow: ToolWindow? = toolWindowManager?.getToolWindow("BacklogPullRequestCheck")

                if(myToolWindow != null){
                    val content = ContentFactory.getInstance().createContent(myToolWindow!!.getContent(), null, false)
                    val contentManager = toolWindow?.contentManager!!
                    contentManager.removeAllContents(true)
                    contentManager.addContent(content)
               }
            }
        }
    }

    fun testConfigValues(workspaceName: String, apiKey: String): Boolean {
        if(workspaceName == "" || apiKey == ""){
            return false
        }
        val configure: BacklogConfigure = BacklogJpConfigure(workspaceName).apiKey(apiKey)
        val newClient: BacklogClient = BacklogClientFactory(configure).newClient()
        if(newClient.myself.name != null){
            backlogClient = newClient
            requestToolWindowUpdate()
            return true
        }
        return false
    }

    fun getRandomNumber() = (1..100).random()
}
