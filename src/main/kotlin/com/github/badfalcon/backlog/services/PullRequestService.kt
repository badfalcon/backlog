package com.github.badfalcon.backlog.services

import com.github.badfalcon.backlog.toolWindow.MyToolWindowFactory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import com.nulabinc.backlog4j.PullRequest
import com.nulabinc.backlog4j.ResponseList
import com.nulabinc.backlog4j.api.option.PullRequestQueryParams
import git4idea.fetch.GitFetchSupport
import javax.swing.JList

@Service(Service.Level.PROJECT)
class PullRequestService(private var project: Project) {
    var backlogService: BacklogService? = null
    var gitService: GitService? = null

    init {
        backlogService = project.service<BacklogService>()
        gitService = project.service<GitService>()
    }

    fun getPullRequests(): ResponseList<PullRequest>? {
        var result : ResponseList<PullRequest>? = null
        if (backlogService?.isReady == true && gitService?.isReady == true) {
            ApplicationManager.getApplication().invokeLater {
                gitService!!.fetch()
                thisLogger().debug("[BLPL]invokeLater")
                val remoteUrl = gitService!!.getRemoteUrl()
                val toolWindow =
                    ToolWindowManager.getInstance(project).getToolWindow(MyToolWindowFactory.TOOL_WINDOW_ID)
                result = backlogService!!.getPullRequests(remoteUrl!!)
                /*
                if (prs != null) {
                    val content = ContentFactory.getInstance().createContent(prs.toString(), null, false)
                    toolWindow.contentManager.removeAllContents(true)
                    toolWindow.contentManager.addContent(content true :turn)
                }
                backlogService!!.getPullRequests(remoteUrl!!)
                return prs
*/

            }
        }
        return result
//                    val pullRequestParams: PullRequestQueryParams = PullRequestQueryParams()
//                    val pullRequestStatusTypes: List<PullRequest.StatusType> =
//                        List<PullRequest.StatusType>(1) { PullRequest.StatusType.Open };
//                    pullRequestParams.statusType(pullRequestStatusTypes)
//                    val pullRequests = backlogService!!.getPullRequests(blProjectKey, blRepoId, pullRequestParams)
            /*
                            var blProjectKey : String? = null
            var blRepoId : Long? = null
            projectLoop@ for (proj in backlog.projects)
            {
                var repositories : ResponseList<Repository>? = null;
                try {
                    repositories = backlog.getGitRepositories(proj.projectKey)
                } catch (e:Exception){
                    continue
                }
                for (repo in repositories)
                {
                    if (repo.httpUrl == targetRemoteUrl)
                    {
                        blProjectKey = proj.projectKey
                        blRepoId = repo.id
                        break@projectLoop
                    }
                }
            }

             */


//                var toolWindow: ToolWindow? = toolWindowManager?.getToolWindow(MyToolWindowFactory.TOOL_WINDOW_ID)
            /*                if(toolWindow != null){
                val content = ContentFactory.getInstance().createContent(toolWindow!!.getContent(), null, false)
                val contentManager = toolWindow?.contentManager!!
                contentManager.removeAllContents(true)
                contentManager.addContent(content)
            }*/
    }
}