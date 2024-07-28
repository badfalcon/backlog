package com.github.badfalcon.backlog.service

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
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.nulabinc.backlog4j.PullRequest
import git4idea.GitCommit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.swing.SwingUtilities

@Service(Service.Level.PROJECT)
class ToolWindowService(private var project: Project, private val cs: CoroutineScope) {
    lateinit var toolWindow: ToolWindow

    var pullRequestService: PullRequestService

    var homeTab: BacklogHomeTab
    var tabs: MutableMap<Long, Content> = mutableMapOf()

    init {
        thisLogger().warn("[backlog] " + "ToolWindowService.init")
        pullRequestService = project.service<PullRequestService>()

//        getPullRequests()
        // subscribe to update topic
        project.messageBus.connect().subscribe(
            UPDATE_TOPIC,
            object : ToolWindowNotifier {
                override fun update(message: String) {
                    println("Received message: $message")
                    getPullRequests()
                }
            })

        // create home tab
        val pullRequestListener = object : PullRequestSelectionListener {
            override fun onPullRequestSelected(pullRequest: PullRequest) {
                println("Selected Pull Request: ${pullRequest.summary}")
                tryGetPullRequestTabContent(pullRequest)
            }
        }
        homeTab = BacklogHomeTab(pullRequestListener)

        // set to tool window
        val window = ToolWindowManager.getInstance(project).getToolWindow("Backlog")
        if (window != null) {
            toolWindow = window

            val homeTab = homeTab.getContent()
            val contentFactory = ContentFactory.getInstance()
            val tabTitle = MyBundle.message("toolWindowHomeTabTitle")
            val content = contentFactory.createContent(homeTab, tabTitle, false)
            content.isCloseable = false

            toolWindow.contentManager.addContent(content)
        }

    }

    fun getPullRequests() {
        thisLogger().warn("[backlog] " + "ToolWindowService.getPullRequests")
        cs.launch {
            withContext(Dispatchers.IO) {
                val pullRequests = pullRequestService.getPullRequests()
                if (pullRequests != null) {
                    pullRequests.reverse()
                    val contentManager = toolWindow.contentManager
                    val tabTitle = MyBundle.message("toolWindowHomeTabTitle")
                    val homeTab = contentManager.findContent(tabTitle).component as BacklogHomeTab
                    homeTab.update(pullRequests)
                }
            }
        }
    }

    fun tryGetPullRequestTabContent(pullRequest: PullRequest) {
        thisLogger().warn("[backlog] " + "ToolWindowService.tryGetPullRequestTabContent")
        val contentManager = toolWindow.contentManager
        if (contentManager.findContent(pullRequest.number.toString()) != null) {
            // select tab
            val content = contentManager.findContent(pullRequest.number.toString())!!
            contentManager.setSelectedContent(content, true, true)
        } else {
            // create tab
            createPullRequestTabContent(pullRequest)
        }
    }

    private val diffListener = object : DiffSelectionListener {
        override fun onDiffSelected(change: Change) {
            showDiff(change)
        }
    }
    private val commitListener = object : CommitSelectionListener {
        override fun onCommitSelected(commit: GitCommit) {
            thisLogger().warn("Commit selected")
            showCommit(commit)
        }
    }

    fun createPullRequestTabContent(pullRequest: PullRequest) {
        thisLogger().warn("[backlog] " + "ToolWindowService.createPullRequestTabContent")
        cs.launch {
            withContext(Dispatchers.IO) {
                val changes = pullRequestService.getChanges(pullRequest)
                val commits = pullRequestService.getCommits(pullRequest)
                val attachments = pullRequestService.getAttachments(pullRequest)
                ApplicationManager.getApplication().invokeLater {
                    val tabContent =
                        BacklogPRDetailTab(
                            pullRequest,
                            project.basePath,
                            changes,
                            diffListener,
                            commits,
                            commitListener,
                            pullRequest.attachments,
                            attachments
                        ).create()
                    val content =
                        ContentFactory.getInstance().createContent(tabContent, pullRequest.number.toString(), false)
                    content.setDisposer { tabs.remove(pullRequest.number) }
                    val contentManager = toolWindow.contentManager
                    thisLogger().warn(content.isCloseable.toString())
                    contentManager.addContent(content)
                    contentManager.setSelectedContent(content, true, true)
                    tabs[pullRequest.number] = content
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
