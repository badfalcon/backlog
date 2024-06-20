package com.github.badfalcon.backlog.services;

import com.github.badfalcon.backlog.config.MyPluginSettingsState
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project
import com.nulabinc.backlog4j.*
import com.nulabinc.backlog4j.api.option.PullRequestQueryParams
import com.nulabinc.backlog4j.conf.BacklogConfigure
import com.nulabinc.backlog4j.conf.BacklogJpConfigure

@Service(Service.Level.PROJECT)
public class BacklogService(private var project: Project) {
    var backlogClient: BacklogClient? = null
    var isReady: Boolean = backlogClient != null
    var projectKey: String = ""
    var repoId: Long = 0

    init {
        val settings: MyPluginSettingsState = MyPluginSettingsState.getInstance()

        if (settings.apiKey != "" && settings.workspaceName != "") {
            val configure: BacklogConfigure = BacklogJpConfigure(settings.workspaceName).apiKey(settings.apiKey);
            if (isValidBacklogConfigs(settings.workspaceName, settings.apiKey)) {
                backlogClient = BacklogClientFactory(configure).newClient()
            }
        }
    }

    /**
     * Checks if the passed values are valid for backlog
     */
    fun isValidBacklogConfigs(workspaceName: String, apiKey: String): Boolean {
        if (workspaceName == "" || apiKey == "") {
            return false
        }
        val configure: BacklogConfigure = BacklogJpConfigure(workspaceName).apiKey(apiKey)
        val newClient: BacklogClient = BacklogClientFactory(configure).newClient()
        if (newClient.myself.name != null) {
            backlogClient = newClient
//            requestToolWindowUpdate()
            return true
        }
        return false
    }

    fun getPullRequests(targetRemoteUrl: String) : ResponseList<PullRequest>?{
        if (isReady) {
            projectLoop@ for (proj in backlogClient!!.projects) {
                var repositories: ResponseList<Repository>? = null;
                try {
                    repositories = backlogClient!!.getGitRepositories(proj.projectKey)
                } catch (e: Exception) {
                    continue
                }
                for (repo in repositories) {
                    if (repo.httpUrl == targetRemoteUrl) {
                        projectKey = proj.projectKey
                        repoId = repo.id
                        break@projectLoop
                    }
                }
            }
            val pullRequestParams: PullRequestQueryParams = PullRequestQueryParams()
            val pullRequestStatusTypes: List<PullRequest.StatusType> =
                List<PullRequest.StatusType>(1) { PullRequest.StatusType.Open };
            pullRequestParams.statusType(pullRequestStatusTypes)
            val pullRequests = backlogClient!!.getPullRequests(projectKey, repoId, pullRequestParams)
            return pullRequests;
        }
        return null
    }
}
