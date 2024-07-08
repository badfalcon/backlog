package com.github.badfalcon.backlog.services

import com.github.badfalcon.backlog.MyBundle
import com.github.badfalcon.backlog.notifier.ToolWindowNotifier
import com.github.badfalcon.backlog.notifier.UPDATE_TOPIC
import com.github.badfalcon.backlog.tabs.*
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.contents.DiffContent
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.nulabinc.backlog4j.PullRequest
import com.nulabinc.backlog4j.ResponseList
import git4idea.GitCommit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.swing.SwingUtilities

@Service(Service.Level.PROJECT)
class ToolWindowService(private var project: Project, private val cs: CoroutineScope) {
    var toolWindow: ToolWindow? = null

    var homeTabContent: BacklogHomeTab? = null
    var tabs: MutableMap<Long, Content> = mutableMapOf()

    var pullRequestService: PullRequestService? = null

    init {
        thisLogger().warn("[backlog] " + "ToolWindowService.init")
        pullRequestService = project.service<PullRequestService>()


        // create listener
        val listener = object : PullRequestSelectionListener {
            override fun onPullRequestSelected(pullRequest: PullRequest) {
                println("Selected Pull Request: ${pullRequest.summary}")
                tryGetPullRequestTabContent(pullRequest)
            }
        }
        homeTabContent = BacklogHomeTab(listener)

        getPullRequests()

        project.messageBus.connect().subscribe(
            UPDATE_TOPIC,
            object : ToolWindowNotifier {
                override fun update(message: String) {
                    // 受信したイベントを処理
                    println("Received message: $message")
                    getPullRequests()
                }
            })
    }

    fun getPullRequests() {
        thisLogger().warn("[backlog] " + "ToolWindowService.getPullRequests")
        cs.launch {
            withContext(Dispatchers.IO) {
                val pullRequests = pullRequestService?.getPullRequests()
                if (pullRequests != null) {
                    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Backlog")
                    val contentManager = toolWindow!!.contentManager
                    val homeTab = contentManager.findContent(MyBundle.message("toolWindowHomeTabTitle")).component as BacklogHomeTab
                    homeTab.update(pullRequests)
                }
            }
        }
    }

    fun createHomeTabContent(pullRequests: ResponseList<PullRequest>? = null): JBPanel<JBPanel<*>> {
        thisLogger().warn("[backlog] " + "ToolWindowService.CreateHomeTab")
        if (toolWindow != null) {
            // ツールウィンドウを取得
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Backlog")
            if (toolWindow != null) {
                // UIスレッドで実行
                ApplicationManager.getApplication().invokeLater {
                    val panel = toolWindow.contentManager.getContent(0)!!.component as BacklogHomeTab
                    panel.update(pullRequests)
                }
            }
        }

        return homeTabContent!!.getContent()
    }

    fun tryGetPullRequestTabContent(pullRequest: PullRequest) {
        thisLogger().warn("[backlog] " + "ToolWindowService.tryGetPullRequestTabContent")
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Backlog")
        val contentManager = toolWindow!!.contentManager
        if (contentManager.findContent(pullRequest.number.toString()) != null) {
            // select tab
            val content = contentManager.findContent(pullRequest.number.toString())!!
            toolWindow.contentManager.setSelectedContent(content, true, true)
        } else {
            // create tab
            createPullRequestTabContent(pullRequest)
        }
    }

    fun createPullRequestTabContent(pullRequest: PullRequest) {
        thisLogger().warn("[backlog] " + "ToolWindowService.createPullRequestTabContent")
        if (toolWindow != null) {
            // コルーチンを使って変更を取得
            cs.launch {
                withContext(Dispatchers.IO) {
                    val changes = pullRequestService?.getChanges(pullRequest)
                    val commits = pullRequestService?.getCommits(pullRequest)
                    ApplicationManager.getApplication().invokeLater {
                        val diffListener = object : DiffSelectionListener {
                            override fun onDiffSelected(change: Change) {
                                showDiff(change)
                            }
                        }
                        val commitListener = object : CommitSelectionListener {
                            override fun onCommitSelected(commit: GitCommit) {
                                thisLogger().warn("Commit selected")
                                showCommit(commit)
                            }
                        }
                        val tabContent =
                            BacklogPRDetailTab(pullRequest, changes, diffListener, commits, commitListener).create()
                        val content =
                            ContentFactory.getInstance().createContent(tabContent, pullRequest.number.toString(), false)
                        content.setDisposer { tabs.remove(pullRequest.number) }
                        val contentManager = toolWindow!!.contentManager
                        thisLogger().warn(content.isCloseable.toString())
                        contentManager.addContent(content)
                        contentManager.setSelectedContent(content, true, true)
                        tabs[pullRequest.number] = content
                    }
                }
            }
        }
    }

    fun showDiff(change: Change) {
        val fileName = change.afterRevision?.file?.name ?: change.beforeRevision?.file?.name ?: "Unknown File"
        val request = createDiffRequest(change, fileName)
        request?.let {
            SwingUtilities.invokeLater {
                DiffManager.getInstance().showDiff(project, it)
            }
        }
    }

    fun showCommit(commit: GitCommit) {
        val diffRequests = commit.changes.mapIndexedNotNull { idx, change ->
            val shortId = commit.id.toShortString()
            val fileName = change.afterRevision?.file?.name ?: change.beforeRevision?.file?.name ?: "Unknown File"
            val title = "$shortId diff ${idx + 1}/${commit.changes.size} ($fileName)"
            createDiffRequest(change, title)
        }

        val diffRequestChain = SimpleDiffRequestChain(diffRequests)
        SwingUtilities.invokeLater {
            DiffManager.getInstance().showDiff(project, diffRequestChain, DiffDialogHints.DEFAULT)
        }
    }

    private fun createDiffRequest(change: Change, title: String): SimpleDiffRequest? {
        val beforeRevision = change.beforeRevision?.content ?: ""
        val afterRevision = change.afterRevision?.content ?: ""
        val fileType = change.beforeRevision?.file?.fileType ?: change.afterRevision?.file?.fileType ?: return null

        val beforeRevisionNumberString = change.beforeRevision?.revisionNumber?.toString() ?: ""
        val afterRevisionNumberString = change.afterRevision?.revisionNumber?.toString() ?: ""

        val currentContent: DiffContent = DiffContentFactory.getInstance().create(project, beforeRevision, fileType)
        val baseContent: DiffContent = DiffContentFactory.getInstance().create(project, afterRevision, fileType)

        return SimpleDiffRequest(
            title,
            currentContent,
            baseContent,
            beforeRevisionNumberString,
            afterRevisionNumberString
        )
    }
}
