package com.github.badfalcon.backlog.tabs

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.changes.Change
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import com.nulabinc.backlog4j.PullRequest
import git4idea.GitCommit
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.table.DefaultTableModel
import javax.swing.JTabbedPane

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

    fun reload() {
        thisLogger().warn("[backlog] BacklogPRDetailTab.reload")
        // 明示的にUIをリロードする場合に使用
    }

    private fun createContentPanel(): DialogPanel {
        return if (pullRequest != null) {
            panel {
                row { label("Pull Request") }
                row { label("タイトル: ${pullRequest.summary}") }
                row { label("詳細: ${pullRequest.description}") }
            }
        } else {
            panel {
                row { label("Pull Request") }
                row { label("プルリクエストがありません") }
            }
        }
    }

    fun create(): JComponent {
        val descriptionHtml = toHtml(pullRequest.description)

        val pullRequestPanel = panel {
            row(pullRequest.number.toString()) { label(pullRequest.summary) }
            row(pullRequest.createdUser.name) {}
            separator()
            row { label(descriptionHtml) }
        }

        val changesTable = createChangesTable()
        val changesPanel = JBScrollPane(changesTable)

        val commitsTable = createCommitsTable()
        val commitsPanel = JBScrollPane(commitsTable)

        // create tabbed pane
        val tabbedPane = JTabbedPane()

        tabbedPane.addTab("Pull Request Details", null, pullRequestPanel, "Details of the Pull Request")

        tabbedPane.addTab("File Changes", null, changesPanel, "List of file changes")

        tabbedPane.addTab("Commits", null, commitsPanel, "List of commits")

        // create main panel
        val mainPanel = panel {
            row {
                cell(tabbedPane).align(Align.FILL)
            }
        }

        return mainPanel
    }

    private fun createChangesTable(): JBTable {
        val columnNames = arrayOf("File Status", "File Name")
        val data = changes?.map {
            arrayOf(it.fileStatus.toString(), getFileName(it))
        }?.toTypedArray() ?: arrayOf(arrayOf("no changes", ""))

        val tableModel = DefaultTableModel(data, columnNames)
        val changesTable = JBTable(tableModel)

        changesTable.selectionModel.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val selectedRow = changesTable.selectedRow
                if (selectedRow >= 0 && selectedRow < changes?.size ?: 0) {
                    val change = changes?.elementAt(selectedRow)
                    change?.let { diffSelectionListener.onDiffSelected(it) }
                }
            }
        }

        return changesTable
    }

    private fun createCommitsTable(): JBTable {
        val columnNames = arrayOf("Commit Hash", "Commit Message")

        val data = commits?.map {
            arrayOf(it.id.toShortString(), it.fullMessage)
        }?.toTypedArray() ?: arrayOf(arrayOf("no commits", ""))

        val tableModel = DefaultTableModel(data, columnNames)
        val commitsTable = JBTable(tableModel)
        commitsTable.autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS

        commitsTable.selectionModel.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val selectedRow = commitsTable.selectedRow
                if (selectedRow >= 0 && selectedRow < commits?.size ?: 0) {
                    val commit = commits?.elementAt(selectedRow)
                    commit?.let { commitSelectionListener.onCommitSelected(it) }
                }
            }
        }

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
}

interface DiffSelectionListener {
    fun onDiffSelected(change: Change)
}

interface CommitSelectionListener {
    fun onCommitSelected(commit: GitCommit)
}