package com.github.badfalcon.backlog.tabs

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.changes.Change
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.nulabinc.backlog4j.PullRequest
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.table.DefaultTableModel
import javax.swing.JTabbedPane

class BacklogPRDetailTab(
    private val pullRequest: PullRequest,
    private val changes: MutableCollection<Change>?,
    private val diffSelectionListener: DiffSelectionListener
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

        // コミット一覧を表示するためのダミーデータ（本来は実データを取得して表示する）
        val commitsTable = createCommitsTable()
        val commitsPanel = JBScrollPane(commitsTable)

        // タブの作成
        val tabbedPane = JTabbedPane()

        // プルリクエストの詳細タブ
        tabbedPane.addTab("Pull Request Details", null, pullRequestPanel, "Details of the Pull Request")

        // ファイル変更タブ
        tabbedPane.addTab("File Changes", null, changesPanel, "List of file changes")

        // コミット一覧タブ
        tabbedPane.addTab("Commits", null, commitsPanel, "List of commits")

        // メインパネルにタブを設定
        val mainPanel = panel {
            row {
                cell(tabbedPane).align(Align.FILL)
            }
        }

        return mainPanel
    }

    private fun createChangesTable(): JTable {
        val columnNames = arrayOf("File Status", "File Name")
        val data = changes?.map {
            arrayOf(it.fileStatus.toString(), getFileName(it))
        }?.toTypedArray() ?: arrayOf(arrayOf("変更がありません", ""))

        val tableModel = DefaultTableModel(data, columnNames)
        val changesTable = JTable(tableModel)

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

    private fun createCommitsTable(): JTable {
        val columnNames = arrayOf("Commit Hash", "Commit Message")
        val data = arrayOf(
            // ダミーデータ
            arrayOf("abc123", "Initial commit"),
            arrayOf("def456", "Fixed bug"),
            arrayOf("ghi789", "Added new feature")
        )

        val tableModel = DefaultTableModel(data, columnNames)
        return JTable(tableModel)
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
