package com.github.badfalcon.backlog.services

import com.github.badfalcon.backlog.notifier.UPDATE_TOPIC
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import git4idea.GitRevisionNumber
import git4idea.GitUtil
import git4idea.changes.GitChangeUtils
import git4idea.fetch.GitFetchSupport
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener

@Service(Service.Level.PROJECT)
class GitService(private var project: Project) {
    var repository: GitRepository? = null
    val isReady: Boolean get() = repository != null

    init {
        thisLogger().warn("[backlog] " + "GitService.init")
        project.messageBus.connect().subscribe(GitRepository.GIT_REPO_CHANGE, GitRepositoryChangeListener {
            checkRepositoryReady()
        })
    }

    fun checkRepositoryReady() {
        thisLogger().warn("[backlog] " + "GitService.checkRepositoryReady")
        val manager = GitUtil.getRepositoryManager(project)
        val basePath: String? = project.basePath
        if (basePath == null) {
            println("!!!!!basePath == null")
            repository = null
            return
        }

        val rootDirectory: VirtualFile? = LocalFileSystem.getInstance().findFileByPath(basePath);
        if (rootDirectory == null) {
            println("!!!!!rootDirectory == null")
            repository = null
            return
        }

        val repo: GitRepository? = manager.getRepositoryForRoot(rootDirectory)
        if (repo == null) {
            println("!!!!!repo == null")
            repository = null
            return
        }
        repository = repo
        println("!!!!!repo ready")
        thisLogger().warn("[BLPL]repository")
        val messageBus = project.messageBus
        val publisher = messageBus.syncPublisher(UPDATE_TOPIC)
        // todo invoke later
        ReadAction.run<Throwable> {
            thisLogger().warn("[backlog] " + "Git repository ready")
            publisher.update("Git repository ready")
        }
    }

    fun fetch() {
        thisLogger().warn("[backlog] " + "GitService.fetch")
        if (isReady) {
            val fetch = GitFetchSupport.fetchSupport(project)
            fetch.fetchAllRemotes(listOf(repository)).showNotificationIfFailed()
        }
    }

    fun getRemoteUrl(): String? {
        thisLogger().warn("[backlog] " + "GitService.getRemoteUrl")
        var result: String? = null
        if (isReady) {
            for (remote in repository!!.remotes) {
                if (remote.firstUrl == null) {
                    continue
                }
                println(remote.firstUrl)
                val backlogUrlRegex = Regex("https://.+\\.jp/git/.+/.+\\.git")
                if (backlogUrlRegex.containsMatchIn(remote.firstUrl!!)) {
                    result = remote.firstUrl
                    break
                }
            }
        }
        return result
    }

    fun getChanges(baseBranchName: String, targetBranchName: String): MutableCollection<Change>? {
        thisLogger().warn("[backlog] " + "GitService.getChanges")
        if (isReady) {
            val base = repository!!.branches.remoteBranches.first { it.nameForRemoteOperations == baseBranchName }
            val target = repository!!.branches.remoteBranches.first { it.nameForRemoteOperations == targetBranchName }

            if (base != null && target != null) {
                // get revisions
                val revisionBase = GitRevisionNumber.resolve(project, repository!!.root, base.name)
                val revisionTarget = GitRevisionNumber.resolve(project, repository!!.root, target.name)
                return GitChangeUtils.getDiff(
                    repository!!.project,
                    repository!!.root,
                    revisionBase.rev,
                    revisionTarget.rev,
                    null
                );
            }
        }
        return null
    }
}