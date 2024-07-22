package com.github.badfalcon.backlog.service

import com.github.badfalcon.backlog.config.MyPluginSettingsState
import com.github.badfalcon.backlog.notifier.UPDATE_TOPIC
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.nulabinc.backlog4j.*
import com.nulabinc.backlog4j.api.option.PullRequestQueryParams
import com.nulabinc.backlog4j.conf.BacklogConfigure
import com.nulabinc.backlog4j.conf.BacklogJpConfigure

@Service(Service.Level.PROJECT)
class BacklogService(project: Project) {
    var backlogClient: BacklogClient? = null
    val isReady: Boolean
        get() = backlogClient != null
    var projectKey: String = ""
    var repoId: Long = 0

    init {
        thisLogger().warn("[backlog] "+ "BacklogService.init")

        val settings: MyPluginSettingsState = MyPluginSettingsState.getInstance()

        if (settings.apiKey != "" && settings.workspaceName != "") {
            val configure: BacklogConfigure = BacklogJpConfigure(settings.workspaceName).apiKey(settings.apiKey)
            if (isValidBacklogConfigs(settings.workspaceName, settings.apiKey)) {
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
    fun isValidBacklogConfigs(workspaceName: String, apiKey: String): Boolean {
        thisLogger().warn("[backlog] "+ "BacklogService.isValidBacklogConfigs")
        if (workspaceName == "" || apiKey == "") {
            return false
        }
        val configure: BacklogConfigure = BacklogJpConfigure(workspaceName).apiKey(apiKey)
        val newClient: BacklogClient = BacklogClientFactory(configure).newClient()
        if (newClient.myself.name != null) {
            backlogClient = newClient
            return true
        }
        return false
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
}
