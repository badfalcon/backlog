package com.github.badfalcon.backlog.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.nulabinc.backlog4j.AttachmentData
import com.nulabinc.backlog4j.PullRequest
import com.nulabinc.backlog4j.ResponseList
import git4idea.GitCommit

@Service(Service.Level.PROJECT)
class PullRequestService(private var project: Project) {
    var backlogService: BacklogService? = null
    var gitService: GitService? = null

    init {
        thisLogger().warn("[backlog] "+"PullRequestService.init")
        backlogService = project.service<BacklogService>()
        gitService = project.service<GitService>()
    }

    fun getPullRequests(): ResponseList<PullRequest>? {
        thisLogger().warn("[backlog] "+"PullRequestService.getPullRequests")
        var result : ResponseList<PullRequest>? = null
        if (backlogService?.isReady == true && gitService?.isReady == true) {
            gitService!!.fetch()
            thisLogger().warn("[BLPL]invokeLater")
            val remoteUrl = gitService!!.getRemoteUrl()
            result = backlogService!!.getPullRequests(remoteUrl!!)
        }
        return result
    }

    fun getChanges(pullRequest: PullRequest) : MutableCollection<Change>?{
        thisLogger().warn("[backlog] "+"PullRequestService.getChanges")
        if(gitService?.isReady == true){
            val changes = gitService!!.getChanges(pullRequest.base, pullRequest.branch)
            return changes
        }
        return null
    }

    fun getCommits(pullRequest: PullRequest) : MutableList<GitCommit>? {
        thisLogger().warn("[backlog] "+"PullRequestService.getCommits")
        if(gitService?.isReady == true){
            val commits = gitService!!.getCommits(pullRequest.base, pullRequest.branch)
            return commits
        }
        return null
    }

    fun getAttachments(pullRequest: PullRequest) : MutableList<AttachmentData>? {
        thisLogger().warn("[backlog] "+"PullRequestService.getAttachments")
        val attachmentData = backlogService!!.getImageAttachments(pullRequest.number, pullRequest.attachments)
        return attachmentData
    }

}