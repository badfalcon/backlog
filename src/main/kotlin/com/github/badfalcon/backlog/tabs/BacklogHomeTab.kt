package com.github.badfalcon.backlog.tabs

import com.github.badfalcon.backlog.notifier.UPDATE_TOPIC
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import com.nulabinc.backlog4j.PullRequest
import com.nulabinc.backlog4j.ResponseList
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JButton
import javax.swing.ListSelectionModel
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

class BacklogHomeTab(private val pullRequestSelectionListener: PullRequestSelectionListener) : JBPanel<JBPanel<*>>() {

    val reloadButton: JButton = JButton("reload")
    val statusLabel: JBLabel = JBLabel()

    var pullRequestTable: JBTable? = null

    init {
        thisLogger().warn("[backlog] " + "BacklogHomeTab.init")
        reloadButton.apply {
            addActionListener {
                statusLabel.text = "reloading"

                // update window
                val project = ProjectManager.getInstance().openProjects[0]
                val messageBus = project.messageBus
                val publisher = messageBus.syncPublisher(UPDATE_TOPIC)
                publisher.update("reload")
            }
        }
        val pullRequests: ResponseList<PullRequest>? = null
        this.update(pullRequests)

        this.layout = BorderLayout()
        reload()
    }

    fun getContent() = this

    fun reload(updateFinish: Boolean = false) {
        thisLogger().warn("[backlog] " + "BacklogHomeTab.reload")
        removeAll()
        val scrollableTable = JBScrollPane(pullRequestTable)
        val mainPanel = panel {
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
        }
        // update window
        revalidate()
        repaint()
    }

    fun update(pullRequests: ResponseList<PullRequest>?) {
        thisLogger().warn("[backlog] " + "BacklogHomeTab.update")
        val columnNames = arrayOf("#", "title", "author", "branch")
        val data = pullRequests?.map {
            arrayOf(it.number.toString(), it.summary, it.createdUser.name, it.branch.toString())
        }?.toTypedArray() ?: arrayOf(arrayOf("", "", "", ""))

        val tableModel = DefaultTableModel(data, columnNames)
        pullRequestTable = JBTable(tableModel)

        // set selection listener
        pullRequestTable!!.selectionModel.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val lsm: ListSelectionModel = e.source as ListSelectionModel
                if (!lsm.isSelectionEmpty) {
                    val selectedRow = lsm.minSelectionIndex
                    pullRequestSelectionListener.onPullRequestSelected(pullRequests!![selectedRow])
                }
            }
        }

        // auto resize columns
        autoResizeTableColumns(pullRequestTable!!)

        reload(true)
    }

    private fun autoResizeTableColumns(table: JBTable) {
        val header = table.tableHeader
        val columnModel = table.columnModel
        for (column in 0 until columnModel.columnCount) {
            var maxWidth = header.getDefaultRenderer()
                .getTableCellRendererComponent(
                    table,
                    header.getColumnModel().getColumn(column).getHeaderValue(),
                    false,
                    false,
                    -1,
                    column
                )
                .preferredSize.width
            for (row in 0 until table.rowCount) {
                val cellRenderer: TableCellRenderer = table.getCellRenderer(row, column)
                val cellComponent: Component = table.prepareRenderer(cellRenderer, row, column)
                val cellWidth: Int = cellComponent.preferredSize.width
                maxWidth = maxOf(maxWidth, cellWidth)
            }
            columnModel.getColumn(column).preferredWidth = maxWidth + 20 // マージンを追加
        }
    }
}

interface PullRequestSelectionListener {
    fun onPullRequestSelected(pullRequest: PullRequest)
}