package com.github.badfalcon.backlog.tabs

import com.github.badfalcon.backlog.BacklogBundle
import com.github.badfalcon.backlog.util.BacklogMarkdownConverter
import com.github.badfalcon.backlog.util.TableUtils
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.changes.Change
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.HtmlPanel
import com.nulabinc.backlog4j.Attachment
import com.nulabinc.backlog4j.AttachmentData
import com.nulabinc.backlog4j.PullRequest
import git4idea.GitCommit
import java.awt.BorderLayout
import javax.swing.JTable
import javax.swing.table.DefaultTableModel
import javax.swing.ListSelectionModel
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

class BacklogPRDetailTab(
    private val pullRequest: PullRequest,
    basePathStr : String?,
    changes: MutableCollection<Change>?,
    diffSelectionListener: DiffSelectionListener,
    commits: MutableList<GitCommit>?,
    commitSelectionListener: CommitSelectionListener,
    attachments: MutableList<Attachment>?,
    attachmentData: MutableList<AttachmentData>
) : JBPanel<JBPanel<*>>()  {
    private val basePath = Path(basePathStr?:"")

    init {
        this.layout = BorderLayout()

        // create overview
        val overviewPanel = panel {
            row("#" + pullRequest.number.toString()) {
                label(pullRequest.summary)
            }
            row(BacklogBundle.message("prDetail.overview.author")) {
                label(pullRequest.createdUser.name)
            }
            row(BacklogBundle.message("prDetail.overview.branch")) {
                label(pullRequest.base.toString() + " <- " + pullRequest.branch.toString())
            }
        }

        // create description
        val htmlPanel = BacklogHtmlPanel(pullRequest.description, attachments, attachmentData)
        val pullRequestPanel = panel {
            row { cell(htmlPanel).align(Align.FILL) }
        }.apply { isOpaque = false }
        val prpScrollPane = JBScrollPane(pullRequestPanel)

        // create changes
        val changesTable = createChangesTable(changes, diffSelectionListener)
        val changesScrollPane = JBScrollPane(changesTable)

        // create commits
        val commitsTable = createCommitsTable(commits, commitSelectionListener)
        val commitsScrollPane = JBScrollPane(commitsTable)

        // create tabbed pane
        val tabbedPane = JBTabbedPane()
        tabbedPane.addTab(BacklogBundle.message("prDetail.tab.description"), null, prpScrollPane, BacklogBundle.message("prDetail.tab.description.tooltip"))
        tabbedPane.addTab(BacklogBundle.message("prDetail.tab.changes"), null, changesScrollPane, BacklogBundle.message("prDetail.tab.changes.tooltip"))
        tabbedPane.addTab(BacklogBundle.message("prDetail.tab.commits"), null, commitsScrollPane, BacklogBundle.message("prDetail.tab.commits.tooltip"))

        // create main panel
        val mainPanel = panel {
            row {
                cell(overviewPanel).align(Align.FILL)
            }
            row {
                cell(tabbedPane).align(Align.FILL)
            }.resizableRow()
        }
        add(mainPanel, BorderLayout.CENTER)
    }

    private fun createChangesTable(changes: MutableCollection<Change>?, diffSelectionListener: DiffSelectionListener): JBTable {
        val columnNames = arrayOf(BacklogBundle.message("prDetail.changes.column.status"), BacklogBundle.message("prDetail.changes.column.fileName"))
        val data = changes?.map {
            arrayOf(it.fileStatus.toString(), getFileName(it))
        }?.toTypedArray() ?: emptyArray()

        val tableModel = object : DefaultTableModel(data, columnNames) {
            override fun isCellEditable(row: Int, column: Int): Boolean {
                return false
            }
        }
        val changesTable = JBTable(tableModel)
        changesTable.emptyText.text = BacklogBundle.message("prDetail.changes.empty")

        changesTable.selectionModel.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val lsm: ListSelectionModel = e.source as ListSelectionModel
                if (!lsm.isSelectionEmpty) {
                    val selectedRow = lsm.minSelectionIndex
                    val change = changes?.elementAt(selectedRow) ?: return@addListSelectionListener
                    diffSelectionListener.onDiffSelected(change)
                }
            }
        }

        TableUtils.autoResizeTableColumns(changesTable)

        return changesTable
    }

    private fun createCommitsTable(commits: MutableList<GitCommit>?, commitSelectionListener: CommitSelectionListener): JBTable {
        val columnNames = arrayOf(BacklogBundle.message("prDetail.commits.column.hash"), BacklogBundle.message("prDetail.commits.column.message"))
        val data = commits?.map {
            arrayOf(it.id.toShortString(), it.fullMessage)
        }?.toTypedArray() ?: emptyArray()

        val tableModel = object : DefaultTableModel(data, columnNames) {
            override fun isCellEditable(row: Int, column: Int): Boolean {
                return false
            }
        }
        val commitsTable = JBTable(tableModel)
        commitsTable.emptyText.text = BacklogBundle.message("prDetail.commits.empty")
        commitsTable.autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS

        commitsTable.selectionModel.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val lsm: ListSelectionModel = e.source as ListSelectionModel
                if (!lsm.isSelectionEmpty) {
                    val selectedRow = lsm.minSelectionIndex
                    val commit = commits?.getOrNull(selectedRow) ?: return@addListSelectionListener
                    commitSelectionListener.onCommitSelected(commit)
                }
            }
        }

        TableUtils.autoResizeTableColumns(commitsTable)

        return commitsTable
    }

    private fun getFileName(change: Change): String? {
        val fullPathStr = when (change.fileStatus) {
            FileStatus.ADDED -> change.afterRevision?.file?.path
            else -> change.beforeRevision?.file?.path
        }
        val fullPath = Path(fullPathStr?:"")

        val relativePath = fullPath.relativeTo(basePath)
        return relativePath.pathString
    }

}

interface DiffSelectionListener {
    fun onDiffSelected(change: Change)
}

interface CommitSelectionListener {
    fun onCommitSelected(commit: GitCommit)
}

private class BacklogHtmlPanel(src: String, attachments: MutableList<Attachment>?, attachmentData: MutableList<AttachmentData>) : HtmlPanel(){
    init {
        contentType = "text/html"

        setBody(src, attachments, attachmentData)
        isEditable = false
        update()
    }

    fun setBody(src: String, attachments: MutableList<Attachment>?, attachmentData: MutableList<AttachmentData>) {
        text = BacklogMarkdownConverter().toHtml(src, attachments, attachmentData)
        update()
    }

    override fun getBody(): String {
        return text
    }
}