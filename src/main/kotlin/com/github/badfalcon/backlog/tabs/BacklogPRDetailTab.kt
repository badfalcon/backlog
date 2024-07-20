package com.github.badfalcon.backlog.tabs

import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.changes.Change
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import com.nulabinc.backlog4j.PullRequest
import git4idea.GitCommit
import java.awt.Component
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.table.DefaultTableModel
import javax.swing.ListSelectionModel
import javax.swing.table.TableCellRenderer

class BacklogPRDetailTab(
    private val pullRequest: PullRequest,
    private val changes: MutableCollection<Change>?,
    private val diffSelectionListener: DiffSelectionListener,
    private val commits: MutableList<GitCommit>?,
    private val commitSelectionListener: CommitSelectionListener
) {

    init {
        println("BacklogPRDetailTab init")
    }

    fun create(): JComponent {
        // create overview
        val overviewPanel = panel {
            row("#" + pullRequest.number.toString()) {
                label(pullRequest.summary)
            }
            row("author") {
                label(pullRequest.createdUser.name)
            }
            row("branch") {
                label (pullRequest.base.toString() + " <- " + pullRequest.branch.toString())
            }
        }

        // create description
        val descriptionHtml = toHtml(pullRequest.description)
        val pullRequestPanel = panel {
            row { label(descriptionHtml) }
        }
        val prpScrollPane = JBScrollPane(pullRequestPanel)

        // create changes
        val changesTable = createChangesTable()
        val changesPanel = JBScrollPane(changesTable)
        val chScrollPane = JBScrollPane(changesPanel)

        // create commits
        val commitsTable = createCommitsTable()
        val commitsPanel = JBScrollPane(commitsTable)
        val cmScrollPane = JBScrollPane(commitsPanel)

        // create tabbed pane
        val tabbedPane = JBTabbedPane()
        tabbedPane.addTab("Pull Request Details", null, prpScrollPane, "Details of the Pull Request")
        tabbedPane.addTab("File Changes", null, chScrollPane, "List of file changes")
        tabbedPane.addTab("Commits", null, cmScrollPane, "List of commits")

        // create main panel
        val mainPanel = panel {
            row {
                cell(overviewPanel)
            }
            row {
                cell(tabbedPane).align(Align.FILL)
            }.resizableRow()
        }

        return mainPanel
    }

    private fun createChangesTable(): JBTable {
        val columnNames = arrayOf("File Status", "File Name")
        val data = changes?.map {
            arrayOf(it.fileStatus.toString(), getFileName(it))
        }?.toTypedArray() ?: arrayOf(arrayOf("", ""))

        val tableModel = DefaultTableModel(data, columnNames)
        val changesTable = JBTable(tableModel)

        changesTable.selectionModel.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {

                val lsm : ListSelectionModel = e.source as ListSelectionModel
                if (!lsm.isSelectionEmpty) {
                    val selectedRow = lsm.minSelectionIndex
                    val change = changes!!.elementAt(selectedRow)
                    diffSelectionListener.onDiffSelected(change)
                }
            }
        }

        autoResizeTableColumns(changesTable)
        return changesTable
    }

    private fun createCommitsTable(): JBTable {
        val columnNames = arrayOf("Commit Hash", "Commit Message")
        val data = commits?.map {
            arrayOf(it.id.toShortString(), it.fullMessage)
        }?.toTypedArray() ?: arrayOf(arrayOf("", ""))

        val tableModel = DefaultTableModel(data, columnNames)
        val commitsTable = JBTable(tableModel)
        commitsTable.autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS

        commitsTable.selectionModel.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val lsm : ListSelectionModel = e.source as ListSelectionModel
                if (!lsm.isSelectionEmpty) {
                    val selectedRow = lsm.minSelectionIndex
                    commitSelectionListener.onCommitSelected(commits!![selectedRow])
                }
            }
        }

        autoResizeTableColumns(commitsTable)

        return commitsTable
    }

    private fun getFileName(change: Change): String? {
        return when (change.fileStatus) {
            FileStatus.ADDED -> change.afterRevision?.file?.name
            else -> change.beforeRevision?.file?.name
        }
    }

    fun toHtml(text: String): String {
        var result = text.replace(Regex("""\*\*\*\s?(.*)\r\n"""), "<h3>$1</h3>")
        result = result.replace(Regex("""\*\*\*\s?(.*)\r\n"""), "<h3>$1</h3>")
        result = result.replace(Regex("""\*\*\s?(.*)\r\n"""), "<h2>$1</h2>")
        result = result.replace(Regex("""\*\s?(.*)\r\n"""), "<h1>$1</h1>")
        result = result.replace("\n", "<br>")

        return "<html>$result</html>"
    }

    private fun autoResizeTableColumns(table: JBTable) {
        val header = table.tableHeader
        val columnModel = table.columnModel
        for (column in 0 until columnModel.columnCount) {
            var maxWidth = header.getDefaultRenderer()
                .getTableCellRendererComponent(table, header.getColumnModel().getColumn(column).getHeaderValue(), false, false, -1, column)
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

interface DiffSelectionListener {
    fun onDiffSelected(change: Change)
}

interface CommitSelectionListener {
    fun onCommitSelected(commit: GitCommit)
}