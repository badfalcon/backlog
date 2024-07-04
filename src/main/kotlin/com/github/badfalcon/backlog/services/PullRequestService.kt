package com.github.badfalcon.backlog.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.nulabinc.backlog4j.PullRequest
import com.nulabinc.backlog4j.ResponseList

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
}