package com.github.badfalcon.backlog.toolWindow

import com.github.badfalcon.backlog.MyBundle
import com.github.badfalcon.backlog.config.MyPluginSettingsConfigurable
import com.github.badfalcon.backlog.services.MyProjectService
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.DiffRequestFactory
import com.intellij.diff.contents.DiffContent
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.platform.ide.progress.ModalTaskOwner.project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.nulabinc.backlog4j.PullRequest
import com.nulabinc.backlog4j.Repository
import com.nulabinc.backlog4j.ResponseList
import com.nulabinc.backlog4j.api.option.PullRequestQueryParams
import git4idea.GitBranch
import git4idea.GitRevisionNumber
import git4idea.changes.GitChangeUtils
import git4idea.commands.Git
import git4idea.fetch.GitFetchSupport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.swing.JButton
import javax.swing.JList


class MyToolWindowFactory : ToolWindowFactory {
    init {
        thisLogger().warn("[BLPL]Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        thisLogger().debug("createToolWindowContent")
        println("createToolWindowContent")
        val myToolWindow = MyToolWindow(project, toolWindow)
        val service = toolWindow.project.service<MyProjectService>()
        service.myToolWindow = myToolWindow

        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    public class MyToolWindow(project: Project, toolWindow: ToolWindow) {

        private val service = toolWindow.project.service<MyProjectService>()
        private var jbPanel = JBPanel<JBPanel<*>>()

        init {
            // todo init時はLoadingを表示
            jbPanel = JBPanel<JBPanel<*>>().apply {
                thisLogger().debug("[BLPL]getContent")
                val label = JBLabel("loading")
                add(label)

            }
        }

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            thisLogger().debug("[BLPL]getContent")
            removeAll()
            val backlog = service.backlogClient!!
            val gitRepo = service.repository!!
            println("[BLPL] " + "gitRepo")
            var targetRemoteUrl: String? = null
            for (remote in gitRepo.remotes)
            {
                if (remote.firstUrl == null)
                {
                    continue
                }
                println(remote.firstUrl)
                val backlogUrlRegex = Regex("https://.+\\.jp/git/.+/.+\\.git")
                if(backlogUrlRegex.containsMatchIn(remote.firstUrl!!))
                {
                    targetRemoteUrl = remote.firstUrl
                    break
                }
            }
            if (targetRemoteUrl != null)
            {
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

                if (blProjectKey != null)
                {
                    val pullRequestParams: PullRequestQueryParams = PullRequestQueryParams()
                    val pullRequestStatusTypes: List<PullRequest.StatusType> =
                        List<PullRequest.StatusType>(1) { PullRequest.StatusType.Open };
                    pullRequestParams.statusType(pullRequestStatusTypes)
                    val pullRequests = backlog.getPullRequests(blProjectKey, blRepoId, pullRequestParams)
                    // プルリクエストの一覧を格納するJListを初期化
                    val pullRequestList = JList(pullRequests.map { it.summary }.toTypedArray())

                    // プルリクエストが選択されたときのリスナーを設定
                    pullRequestList.addListSelectionListener { e ->
                        if (!e.valueIsAdjusting) {
                            // todo create a new tab
                            val repository = service.repository!!
                            val fetch = GitFetchSupport.fetchSupport(repository.project)
                            val res = fetch.fetchAllRemotes(listOf(repository)).showNotificationIfFailed()

                            val selectedIndex = pullRequestList.selectedIndex
                            if (selectedIndex != -1) {
                                // プルリクエストの詳細を表示
                                val selectedPullRequest = pullRequests[selectedIndex]

                                gitRepo.branches.findBranchByName(selectedPullRequest.base)
                                val base = gitRepo.branches.remoteBranches.first { it.nameForRemoteOperations == selectedPullRequest.base }
                                val baseBranch : GitBranch? = gitRepo.branches.findBranchByName(selectedPullRequest.base)

                                val target = gitRepo.branches.remoteBranches.first { it.nameForRemoteOperations == selectedPullRequest.branch }
                                val targetBranch : GitBranch? = gitRepo.branches.findBranchByName(selectedPullRequest.branch)

                                if(base != null && target != null)
                                {
                                    val revisionBase = GitRevisionNumber.resolve(repository.project, repository.root, base.name)
                                    val revisionTarget = GitRevisionNumber.resolve(repository.project, repository.root, target.name)
                                    if(revisionBase != null && revisionTarget != null)
                                    {
                                        GlobalScope.launch(Dispatchers.IO) {
                                            val changes = GitChangeUtils.getDiff(
                                                repository.project,
                                                repository.root,
                                                revisionBase.rev,
                                                revisionTarget.rev,
                                                null);
                                            withContext(Dispatchers.Main) {
                                                // 変更を用いてGUIを更新します
                                                changes.forEach{
                                                    println(it.virtualFile?.name)
                                                    println(it.fileStatus.text)
                                                    val beforeRevision = it.beforeRevision?.content
                                                    val afterRevision = it.afterRevision?.content

                                                    // print the difference between the two revisions
                                                    // this is a naive implementation and does not take into account line numbers or contextual differences
                                                }
                                                val change = changes.first()
                                                val beforeRevision = change.beforeRevision?.content!!
                                                val afterRevision = change.afterRevision?.content!!

                                                val currentContent: DiffContent = DiffContentFactory.getInstance().create(beforeRevision)
                                                val baseContent: DiffContent = DiffContentFactory.getInstance().create(afterRevision)
                                                val request = SimpleDiffRequest(change.toString(),currentContent,baseContent,"a","b")
                                                DiffManager.getInstance().showDiff(repository.project, request)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // pullRequestList をパネルに追加
                    add(pullRequestList)
                }else{
                    println("[BLPL] Backlog project Id not found")
                    add(JBLabel("[BLPL] Backlog project Id not found"))
                }

            }else{
                println("[BLPL] VCS repository remote URL not found")
                add(JBLabel("[BLPL] VCS repository remote URL not found"))
            }
        }
    }
}

