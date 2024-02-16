package com.github.badfalcon.backlog.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import git4idea.GitUtil
import git4idea.changes.GitChangeUtils
import git4idea.fetch.GitFetchSupport
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener

@Service(Service.Level.APP)
class GitService(project: Project) {
    var repository : GitRepository? = null
    var isReady : Boolean = repository != null

    init {
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
        })
    }

    fun fetch(){
        if (repository != null){
            val fetch = GitFetchSupport.fetchSupport(repository!!.project)
            fetch.fetchAllRemotes(listOf(repository)).showNotificationIfFailed()
        }
    }

    fun getDiff(baseRevision: String, targetRevision: String): MutableCollection<Change>? {
        if (repository != null){
            return GitChangeUtils.getDiff(
                repository!!.project,
                repository!!.root,
                baseRevision,
                targetRevision,
                null);
        }
        return null
    }
}