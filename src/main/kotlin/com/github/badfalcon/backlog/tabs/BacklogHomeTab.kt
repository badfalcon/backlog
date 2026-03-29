package com.github.badfalcon.backlog.tabs

import com.github.badfalcon.backlog.BacklogBundle
import com.github.badfalcon.backlog.notifier.UPDATE_TOPIC
import com.github.badfalcon.backlog.service.BacklogService
import com.github.badfalcon.backlog.service.GitService
import com.github.badfalcon.backlog.util.TableUtils
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import com.nulabinc.backlog4j.PullRequest
import com.nulabinc.backlog4j.ResponseList
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableModel

class BacklogHomeTab(
    private val project: Project,
    private val parentDisposable: Disposable,
    private val pullRequestSelectionListener: PullRequestSelectionListener
) : JBPanel<JBPanel<*>>() {

    val reloadButton: JButton = JButton(BacklogBundle.message("homeTab.reload")).apply {
        icon = AllIcons.Actions.Refresh
    }
    val statusLabel: JBLabel = JBLabel()
    val searchField: SearchTextField = SearchTextField()
    private val tableLoadingPanel: JBLoadingPanel = JBLoadingPanel(BorderLayout(), parentDisposable)

    var pullRequestTable: JBTable = JBTable(createTableModel(null))

    var pullRequests: ResponseList<PullRequest>? = null
    private var filteredPullRequests: List<PullRequest>? = null

    init {
        thisLogger().warn("[backlog] " + "BacklogHomeTab.init")
        layout = BorderLayout()

        // create pull request selection table
        pullRequestTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        pullRequestTable.emptyText.text = BacklogBundle.message("homeTab.table.empty")
        // set selection listener
        pullRequestTable.selectionModel.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val lsm: ListSelectionModel = e.source as ListSelectionModel
                if (!lsm.isSelectionEmpty) {
                    val selectedRow = lsm.minSelectionIndex
                    val pr = this.filteredPullRequests?.getOrNull(selectedRow) ?: return@addListSelectionListener
                    pullRequestSelectionListener.onPullRequestSelected(pr)
                }
            }
        }

        // create reload button action
        reloadButton.apply {
            addActionListener {
                reloadButton.isEnabled = false
                pullRequestTable.isEnabled = false
                statusLabel.text = BacklogBundle.message("homeTab.status.reloading")
                statusLabel.icon = AnimatedIcon.Default()
                tableLoadingPanel.startLoading()

                // update window
                val messageBus = project.messageBus
                val publisher = messageBus.syncPublisher(UPDATE_TOPIC)
                publisher.update("reload")
            }
        }

        // setup search field filter
        searchField.textEditor.emptyText.text = BacklogBundle.message("homeTab.search.placeholder")
        searchField.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = filterPullRequests()
            override fun removeUpdate(e: DocumentEvent?) = filterPullRequests()
            override fun changedUpdate(e: DocumentEvent?) = filterPullRequests()
        })

        // setup table loading panel (CENTER)
        tableLoadingPanel.add(JBScrollPane(pullRequestTable), BorderLayout.CENTER)
        add(tableLoadingPanel, BorderLayout.CENTER)

        // build toolbar (NORTH)
        rebuildToolbar()

        // start loading on initial fetch
        tableLoadingPanel.startLoading()
    }

    fun getContent() = this

    private fun rebuildToolbar() {
        val backlogReady: Boolean = project.getService(BacklogService::class.java).isReady
        val gitReady: Boolean = project.getService(GitService::class.java).isReady

        val gitStatusIcon = if (gitReady) AllIcons.General.InspectionsOK else AllIcons.General.Error
        val backlogStatusIcon = if (backlogReady) AllIcons.General.InspectionsOK else AllIcons.General.Error

        val toolbarPanel = panel {
            row {
                cell(JBLabel("Git"))
                cell(JBLabel(if (gitReady) BacklogBundle.message("homeTab.status.ready") else BacklogBundle.message("homeTab.status.notReady"), gitStatusIcon, JBLabel.LEFT))
            }.layout(RowLayout.LABEL_ALIGNED)
            row {
                cell(JBLabel("Backlog"))
                cell(JBLabel(if (backlogReady) BacklogBundle.message("homeTab.status.ready") else BacklogBundle.message("homeTab.status.notReady"), backlogStatusIcon, JBLabel.LEFT))
            }.layout(RowLayout.LABEL_ALIGNED)
            row {
                cell(reloadButton)
                cell(statusLabel)
            }
            row {
                cell(searchField).align(AlignX.FILL)
            }
        }

        // replace NORTH component
        (layout as BorderLayout).getLayoutComponent(BorderLayout.NORTH)?.let { remove(it) }
        add(toolbarPanel, BorderLayout.NORTH)
    }

    fun update(pullRequests: ResponseList<PullRequest>?) {
        thisLogger().warn("[backlog] " + "BacklogHomeTab.update")
        this.pullRequests = pullRequests
        this.filteredPullRequests = pullRequests?.toList()
        searchField.text = ""
        pullRequestTable.model = createTableModel(this.filteredPullRequests)
        TableUtils.autoResizeTableColumns(pullRequestTable)
        pullRequestTable.isEnabled = true

        // update toolbar (ready status may have changed)
        rebuildToolbar()

        // stop loading
        statusLabel.text = ""
        statusLabel.icon = null
        statusLabel.foreground = JBColor.foreground()
        reloadButton.isEnabled = true
        tableLoadingPanel.stopLoading()

        revalidate()
        repaint()
    }

    fun showError(message: String) {
        statusLabel.text = BacklogBundle.message("homeTab.status.error", message)
        statusLabel.foreground = JBColor.RED
        statusLabel.icon = null
        reloadButton.isEnabled = true
        pullRequestTable.isEnabled = true
        tableLoadingPanel.stopLoading()
    }

    private fun filterPullRequests() {
        val query = searchField.text.lowercase()
        filteredPullRequests = if (query.isBlank()) {
            pullRequests?.toList()
        } else {
            pullRequests?.filter {
                it.summary.lowercase().contains(query) ||
                        it.createdUser.name.lowercase().contains(query) ||
                        it.branch.lowercase().contains(query) ||
                        it.number.toString().contains(query)
            }
        }
        pullRequestTable.model = createTableModel(filteredPullRequests)
        TableUtils.autoResizeTableColumns(pullRequestTable)
    }

    private fun createTableModel(pullRequests: List<PullRequest>?): DefaultTableModel {
        val columnNames = arrayOf(
            "#",
            BacklogBundle.message("homeTab.column.title"),
            BacklogBundle.message("homeTab.column.author"),
            BacklogBundle.message("homeTab.column.branch")
        )
        val data = pullRequests?.map {
            arrayOf(it.number.toString(), it.summary, it.createdUser.name, it.branch.toString())
        }?.toTypedArray() ?: emptyArray()

        return object : DefaultTableModel(data, columnNames) {
            override fun isCellEditable(row: Int, column: Int): Boolean {
                return false
            }
        }
    }
}

interface PullRequestSelectionListener {
    fun onPullRequestSelected(pullRequest: PullRequest)
}
