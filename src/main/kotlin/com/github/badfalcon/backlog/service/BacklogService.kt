package com.github.badfalcon.backlog.service

import com.github.badfalcon.backlog.config.MyPluginSettingsState
import com.github.badfalcon.backlog.notifier.UPDATE_TOPIC
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.nulabinc.backlog4j.*
import com.nulabinc.backlog4j.api.option.AddPullRequestCommentParams
import com.nulabinc.backlog4j.api.option.PullRequestQueryParams
import com.nulabinc.backlog4j.api.option.QueryParams
import com.nulabinc.backlog4j.api.option.UpdatePullRequestParams
import com.nulabinc.backlog4j.conf.BacklogComConfigure
import com.nulabinc.backlog4j.conf.BacklogConfigure
import com.nulabinc.backlog4j.conf.BacklogJpConfigure
import com.nulabinc.backlog4j.http.NameValuePair

@Service(Service.Level.PROJECT)
class BacklogService(project: Project) {
    var backlogClient: BacklogClient? = null
    val isReady: Boolean
        get() = backlogClient != null
    var projectKey: String = ""
    var repoId: Long = 0

    enum class TopLevelDomain(val value: String) {
        COM("com"),
        JP("jp"),
    }

    init {
        thisLogger().warn("[backlog] "+ "BacklogService.init")

        val settings: MyPluginSettingsState = MyPluginSettingsState.getInstance(project)

        if (settings.apiKey != "" && settings.workspaceName != "") {
            val configure: BacklogConfigure? = isValidBacklogConfigs(settings.workspaceName, settings.apiKey, settings.topLevelDomain)
            if (configure != null) {
                backlogClient = BacklogClientFactory(configure).newClient()
                val messageBus = project.messageBus
                val publisher = messageBus.syncPublisher(UPDATE_TOPIC)
                publisher.update("Backlog client is ready")
            }
        }
    }

    /**
     * Checks if the passed values are valid for backlog
     */
    fun isValidBacklogConfigs(workspaceName: String, apiKey: String, topLevelDomain: TopLevelDomain): BacklogConfigure? {
        thisLogger().warn("[backlog] "+ "BacklogService.isValidBacklogConfigs")
        if (workspaceName == "" || apiKey == "") {
            return null
        }

        val configure: BacklogConfigure = when (topLevelDomain) {
            TopLevelDomain.JP -> {
                BacklogJpConfigure(workspaceName).apiKey(apiKey)
            }

            TopLevelDomain.COM -> {
                BacklogComConfigure(workspaceName).apiKey(apiKey)
            }
        }
        val newClient: BacklogClient = BacklogClientFactory(configure).newClient()
        try {
            if (newClient.myself.name != null) {
                backlogClient = newClient
                return configure
            }
        } catch (e: Exception) {
            return null
        }
        return null
    }

    fun getPullRequests(targetRemoteUrl: String) : ResponseList<PullRequest>?{
        thisLogger().warn("[backlog] "+ "BacklogService.getPullRequests")
        if (isReady) {
            projectLoop@ for (proj in backlogClient!!.projects) {
                var repositories: ResponseList<Repository>? = null
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
            val pullRequestParams = PullRequestQueryParams()
            val pullRequestStatusTypes: List<PullRequest.StatusType> = List(1) { PullRequest.StatusType.Open }
            pullRequestParams.statusType(pullRequestStatusTypes)

            val pullRequests = backlogClient!!.getPullRequests(projectKey, repoId, pullRequestParams)
            return pullRequests
        }
        return null
    }

    fun getImageAttachments(pullRequestId: Long, attachments: MutableList<Attachment>): MutableList<AttachmentData> {
        thisLogger().warn("[backlog] " + "GitService.getAttachmentData")
        val list = mutableListOf<AttachmentData>()
        if (isReady) {
            for (attachment in attachments) {
                val attachmentId = attachment.id
                val data = backlogClient!!.downloadPullRequestAttachment(projectKey, repoId, pullRequestId, attachmentId);
                list.add(data)
            }
        }
        return list
    }

    fun getPullRequestComments(pullRequestNumber: Long): ResponseList<PullRequestComment>? {
        thisLogger().warn("[backlog] " + "BacklogService.getPullRequestComments")
        if (!isReady) return null
        val queryParams = QueryParams()
        queryParams.order(QueryParams.Order.Asc)
        queryParams.count(100)
        return backlogClient!!.getPullRequestComments(projectKey, repoId, pullRequestNumber, queryParams)
    }

    fun addPullRequestComment(pullRequestNumber: Long, content: String): PullRequestComment? {
        thisLogger().warn("[backlog] " + "BacklogService.addPullRequestComment")
        if (!isReady) return null
        val params = AddPullRequestCommentParams(projectKey, repoId, pullRequestNumber, content)
        return backlogClient!!.addPullRequestComment(params)
    }

    fun updatePullRequestStatus(pullRequestNumber: Long, statusType: PullRequest.StatusType): PullRequest? {
        thisLogger().warn("[backlog] " + "BacklogService.updatePullRequestStatus")
        if (!isReady) return null
        val params = UpdatePullRequestWithStatusParams(projectKey, repoId, pullRequestNumber, statusType)
        return backlogClient!!.updatePullRequest(params)
    }
}

private class UpdatePullRequestWithStatusParams(
    projectIdOrKey: Any, repoIdOrName: Any, number: Any, statusType: PullRequest.StatusType
) : UpdatePullRequestParams(projectIdOrKey, repoIdOrName, number) {
    init {
        parameters.add(NameValuePair("statusId", statusType.intValue.toString()))
    }
}
