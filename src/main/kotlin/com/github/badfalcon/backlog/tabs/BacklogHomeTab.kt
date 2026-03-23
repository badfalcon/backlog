package com.github.badfalcon.backlog.tabs

import com.github.badfalcon.backlog.notifier.UPDATE_TOPIC
import com.github.badfalcon.backlog.service.BacklogService
import com.github.badfalcon.backlog.service.GitService
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import com.nulabinc.backlog4j.PullRequest
import com.nulabinc.backlog4j.ResponseList
import com.github.badfalcon.backlog.util.autoResizeTableColumns
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.ListSelectionModel
import javax.swing.table.DefaultTableModel

class BacklogHomeTab(private val project: Project, private val pullRequestSelectionListener: PullRequestSelectionListener) : JBPanel<JBPanel<*>>() {

    val reloadButton: JButton = JButton("reload")
    val statusLabel: JBLabel = JBLabel()

    var pullRequestTable: JBTable = JBTable(createTableModel(null))

    var pullRequests: ResponseList<PullRequest>? = null

    init {
        thisLogger().info("[backlog] " + "BacklogHomeTab.init")
        // create pull request selection table
        pullRequestTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        // set selection listener
        pullRequestTable.selectionModel.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val lsm: ListSelectionModel = e.source as ListSelectionModel
                if (!lsm.isSelectionEmpty) {
                    val selectedRow = lsm.minSelectionIndex
                    val pr = this.pullRequests?.getOrNull(selectedRow) ?: return@addListSelectionListener
                    pullRequestSelectionListener.onPullRequestSelected(pr)
                }
            }
        }

        // create reload button action
        reloadButton.apply {
            addActionListener {
                reloadButton.isEnabled = false
                pullRequestTable.isEnabled = false
                statusLabel.text = "reloading"

                // update window
                val project = project
                val messageBus = project.messageBus
                val publisher = messageBus.syncPublisher(UPDATE_TOPIC)
                publisher.update("reload")
            }
        }
        val pullRequests: ResponseList<PullRequest>? = null
        this.update(pullRequests)

        this.layout = BorderLayout()
        reload(false)
    }

    fun getContent() = this

    fun reload(updateFinish: Boolean = false) {
        thisLogger().info("[backlog] " + "BacklogHomeTab.reload")
        removeAll()
        val scrollableTable = JBScrollPane(pullRequestTable)
        // get project
        val project = project
        // backlogがisReadyならばreadyと表示
        val backlogReady : Boolean = project.service<BacklogService>().isReady
        // gitがisReadyならばreadyと表示
        val gitReady  : Boolean = project.service<GitService>().isReady
        val mainPanel = panel {
            row {
                cell(JBLabel("Git"))
                cell(JBLabel(if(gitReady) "ready" else "not ready"))
            }.layout(RowLayout.LABEL_ALIGNED)
            row {
                cell(JBLabel("Backlog"))
                cell(JBLabel(if(backlogReady) "ready" else "not ready"))
            }.layout(RowLayout.LABEL_ALIGNED)
            row {
                cell(reloadButton)
                cell(statusLabel)
            }

            row {
                cell(scrollableTable).align(Align.FILL)
            }.resizableRow()
        }
        add(mainPanel, BorderLayout.CENTER)

        if (updateFinish) {
            statusLabel.text = ""
            reloadButton.isEnabled = true
            pullRequestTable.isEnabled = true

        }
        // update window
        revalidate()
        repaint()
    }

    fun update(pullRequests: ResponseList<PullRequest>?) {
        thisLogger().info("[backlog] " + "BacklogHomeTab.update")
        this.pullRequests = pullRequests
        pullRequestTable.model = createTableModel(this.pullRequests)

        // auto resize columns
        autoResizeTableColumns(pullRequestTable)

        pullRequestTable.isEnabled = true

        reload(true)
    }

    private fun createTableModel(pullRequests: ResponseList<PullRequest>?): DefaultTableModel {
        val columnNames = arrayOf("#", "title", "author", "branch")
        val data = pullRequests?.map {
            arrayOf(it.number.toString(), it.summary, it.createdUser.name, it.branch.toString())
        }?.toTypedArray() ?: arrayOf(arrayOf("", "", "", ""))

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