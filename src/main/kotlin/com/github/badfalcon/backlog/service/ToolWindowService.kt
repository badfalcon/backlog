package com.github.badfalcon.backlog.service

import com.github.badfalcon.backlog.BacklogBundle
import com.github.badfalcon.backlog.notifier.ToolWindowNotifier
import com.github.badfalcon.backlog.notifier.UPDATE_TOPIC
import com.github.badfalcon.backlog.tabs.*
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.contents.DiffContent
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.panel
import com.nulabinc.backlog4j.PullRequest
import git4idea.GitCommit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel


@Service(Service.Level.PROJECT)
class ToolWindowService(private val project: Project, private val cs: CoroutineScope) {
    lateinit var toolWindow: ToolWindow

    var pullRequestService: PullRequestService

    var homeTab: BacklogHomeTab
    var tabs: MutableMap<Long, Content> = mutableMapOf()

    init {
        thisLogger().warn("[backlog] ToolWindowService.init")
        pullRequestService = project.getService(PullRequestService::class.java)

        // subscribe to update topic
        project.messageBus.connect().subscribe(
            UPDATE_TOPIC,
            object : ToolWindowNotifier {
                override fun update(message: String) {
                    thisLogger().warn("[backlog] Received message: $message")
                    getPullRequests()
                }
            })

        // set to tool window
        val window = ToolWindowManager.getInstance(project).getToolWindow("Backlog")
        val pullRequestListener = object : PullRequestSelectionListener {
            override fun onPullRequestSelected(pullRequest: PullRequest) {
                thisLogger().warn("[backlog] Selected Pull Request: ${pullRequest.summary}")
                tryGetPullRequestTabContent(pullRequest)
            }
        }
        if (window == null) {
            thisLogger().warn("[backlog] ToolWindow 'Backlog' not found, skipping UI initialization")
            homeTab = BacklogHomeTab(project, { }, pullRequestListener)
        } else {
            toolWindow = window

            // create home tab
            homeTab = BacklogHomeTab(project, toolWindow.disposable, pullRequestListener)

            // add home tab to tool window
            val homeTabContent = homeTab.getContent()
            val contentFactory = ContentFactory.getInstance()
            val tabTitle = BacklogBundle.message("toolWindowHomeTabTitle")
            val content = contentFactory.createContent(homeTabContent, tabTitle, false)
            content.isCloseable = false
            toolWindow.contentManager.addContent(content)

            getPullRequests()
        }
    }

    fun getPullRequests() {
        thisLogger().warn("[backlog] ToolWindowService.getPullRequests")
        if (!::toolWindow.isInitialized) {
            thisLogger().warn("[backlog] ToolWindow not initialized, skipping getPullRequests")
            return
        }
        cs.launch {
            try {
                val pullRequests = withContext(Dispatchers.IO) {
                    pullRequestService.getPullRequests()?.apply { reverse() }
                }
                ApplicationManager.getApplication().invokeLater {
                    try {
                        val contentManager = toolWindow.contentManager
                        val tabTitle = BacklogBundle.message("toolWindowHomeTabTitle")
                        val content = contentManager.findContent(tabTitle) ?: return@invokeLater
                        val homeTab = content.component as? BacklogHomeTab ?: return@invokeLater
                        homeTab.update(pullRequests)
                    } catch (e: Exception) {
                        thisLogger().warn("[backlog] Failed to update PR list UI: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                thisLogger().warn("[backlog] Failed to fetch pull requests: ${e.message}", e)
                ApplicationManager.getApplication().invokeLater {
                    homeTab.showError(e.message ?: BacklogBundle.message("error.unknown"))
                }
            }
        }
    }

    fun tryGetPullRequestTabContent(pullRequest: PullRequest) {
        thisLogger().warn("[backlog] ToolWindowService.tryGetPullRequestTabContent")
        if (!::toolWindow.isInitialized) return
        val contentManager = toolWindow.contentManager
        val content = contentManager.findContent(pullRequest.number.toString())
        if (content != null) {
            // select tab
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
            thisLogger().warn("[backlog] Commit selected")
            showCommit(commit)
        }
    }

    fun createPullRequestTabContent(pullRequest: PullRequest) {
        thisLogger().warn("[backlog] ToolWindowService.createPullRequestTabContent")
        if (!::toolWindow.isInitialized) return

        // Show loading tab immediately
        val loadingPanel = JBLoadingPanel(BorderLayout(), toolWindow.disposable)
        loadingPanel.startLoading()
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(loadingPanel, pullRequest.number.toString(), false)
        content.setDisposer { tabs.remove(pullRequest.number) }
        val contentManager = toolWindow.contentManager
        contentManager.addContent(content)
        contentManager.setSelectedContent(content, true, true)
        tabs[pullRequest.number] = content

        cs.launch {
            try {
                val changes = withContext(Dispatchers.IO) { pullRequestService.getChanges(pullRequest) }
                val commits = withContext(Dispatchers.IO) { pullRequestService.getCommits(pullRequest) }
                val attachments = withContext(Dispatchers.IO) { pullRequestService.getAttachments(pullRequest) }
                ApplicationManager.getApplication().invokeLater {
                    try {
                        val prUrl = pullRequestService.backlogService.getPullRequestUrl(project, pullRequest.number)
                        val tabContent = BacklogPRDetailTab(
                            pullRequest,
                            project.basePath,
                            changes,
                            diffListener,
                            commits,
                            commitListener,
                            pullRequest.attachments,
                            attachments,
                            prUrl
                        )
                        loadingPanel.stopLoading()
                        loadingPanel.add(tabContent, BorderLayout.CENTER)
                        loadingPanel.revalidate()
                        loadingPanel.repaint()
                    } catch (e: Exception) {
                        thisLogger().warn("[backlog] Failed to create PR detail tab UI: ${e.message}", e)
                        loadingPanel.stopLoading()
                        loadingPanel.add(createErrorPanel(e.message ?: BacklogBundle.message("error.unknown")) {
                            contentManager.removeContent(content, true)
                            tabs.remove(pullRequest.number)
                            createPullRequestTabContent(pullRequest)
                        }, BorderLayout.CENTER)
                        loadingPanel.revalidate()
                    }
                }
            } catch (e: Exception) {
                thisLogger().warn("[backlog] Failed to fetch PR detail data: ${e.message}", e)
                ApplicationManager.getApplication().invokeLater {
                    loadingPanel.stopLoading()
                    loadingPanel.add(createErrorPanel(e.message ?: BacklogBundle.message("error.unknown")) {
                        contentManager.removeContent(content, true)
                        tabs.remove(pullRequest.number)
                        createPullRequestTabContent(pullRequest)
                    }, BorderLayout.CENTER)
                    loadingPanel.revalidate()
                }
            }
        }
    }

    private fun createErrorPanel(message: String, retryAction: () -> Unit): JPanel {
        val retryButton = JButton(BacklogBundle.message("prDetail.error.retry"))
        retryButton.addActionListener { retryAction() }
        return panel {
            row {
                icon(AllIcons.General.Error)
                label(message)
            }
            row {
                cell(retryButton)
            }
        }
    }

    fun showDiff(change: Change) {
        cs.launch {
            val fileName = change.afterRevision?.file?.name ?: change.beforeRevision?.file?.name ?: BacklogBundle.message("diff.unknownFile")
            val request = withContext(Dispatchers.IO) {
                createDiffRequest(change, fileName)
            }
            request?.let {
                ApplicationManager.getApplication().invokeLater {
                    DiffManager.getInstance().showDiff(project, it)
                }
            }
        }
    }

    @Suppress("UnstableApiUsage")
    fun showCommit(commit: GitCommit) {
        cs.launch {
            val diffRequests = withContext(Dispatchers.IO) {
                commit.changes.mapIndexedNotNull { idx, change ->
                    val shortId = commit.id.toShortString()
                    val fileName = change.afterRevision?.file?.name ?: change.beforeRevision?.file?.name ?: BacklogBundle.message("diff.unknownFile")
                    val title = "$shortId diff ${idx + 1}/${commit.changes.size} ($fileName)"
                    createDiffRequest(change, title)
                }
            }

            val diffRequestChain = SimpleDiffRequestChain(diffRequests)
            ApplicationManager.getApplication().invokeLater {
                DiffManager.getInstance().showDiff(project, diffRequestChain, DiffDialogHints.DEFAULT)
            }
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
