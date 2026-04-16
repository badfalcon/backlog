package com.github.badfalcon.backlog.tabs

import com.github.badfalcon.backlog.BacklogBundle
import com.github.badfalcon.backlog.util.BacklogMarkdownConverter
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
import com.nulabinc.backlog4j.PullRequestComment
import com.nulabinc.backlog4j.ResponseList
import git4idea.GitCommit
import java.awt.BorderLayout
import java.awt.Component
import java.text.SimpleDateFormat
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.ListSelectionModel
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
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
    attachmentData: MutableList<AttachmentData>,
    comments: ResponseList<PullRequestComment>?,
    private val commentPostListener: CommentPostListener,
    private val statusChangeListener: StatusChangeListener
) : JBPanel<JBPanel<*>>()  {
    private val basePath = Path(basePathStr?:"")
    private lateinit var tabbedPane: JBTabbedPane

    init {
        println("BacklogPRDetailTab init")
        this.layout = BorderLayout()

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
            row(BacklogBundle.message("overviewStatus")) {
                label(pullRequest.status.name)
            }
        }

        // create status action buttons
        val statusPanel = JBPanel<JBPanel<*>>()
        when (pullRequest.status.status) {
            PullRequest.StatusType.Open -> {
                val closeButton = JButton(BacklogBundle.message("statusClose"))
                closeButton.addActionListener {
                    closeButton.isEnabled = false
                    statusChangeListener.onStatusChange(pullRequest, PullRequest.StatusType.Closed)
                }
                statusPanel.add(closeButton)
                val mergeButton = JButton(BacklogBundle.message("statusMerge"))
                mergeButton.addActionListener {
                    mergeButton.isEnabled = false
                    statusChangeListener.onStatusChange(pullRequest, PullRequest.StatusType.Merged)
                }
                statusPanel.add(mergeButton)
            }
            PullRequest.StatusType.Closed -> {
                val reopenButton = JButton(BacklogBundle.message("statusReopen"))
                reopenButton.addActionListener {
                    reopenButton.isEnabled = false
                    statusChangeListener.onStatusChange(pullRequest, PullRequest.StatusType.Open)
                }
                statusPanel.add(reopenButton)
            }
            else -> {}
        }

        // create description
        val htmlPanel =  BacklogHtmlPanel(pullRequest.description, attachments, attachmentData)
        val pullRequestPanel = panel {
            row { cell(htmlPanel) }
        }
        val prpScrollPane = JBScrollPane(pullRequestPanel)

        // create changes
        val changesTable = createChangesTable(changes, diffSelectionListener)
        val changesPanel = JBScrollPane(changesTable)
        val chScrollPane = JBScrollPane(changesPanel)

        // create commits
        val commitsTable = createCommitsTable(commits, commitSelectionListener)
        val commitsPanel = JBScrollPane(commitsTable)
        val cmScrollPane = JBScrollPane(commitsPanel)

        // create tabbed pane
        tabbedPane = JBTabbedPane()
        tabbedPane.addTab("Pull Request Details", null, prpScrollPane, "Details of the Pull Request")
        tabbedPane.addTab("File Changes", null, chScrollPane, "List of file changes")
        tabbedPane.addTab("Commits", null, cmScrollPane, "List of commits")
        tabbedPane.addTab(BacklogBundle.message("tabComments"), null, createCommentsPanel(comments), "Pull request comments")

        // create main panel
        val mainPanel = panel {
            row {
                cell(overviewPanel)
            }
            row {
                cell(statusPanel)
            }
            row {
                cell(tabbedPane).align(Align.FILL)
            }.resizableRow()
        }
        add(mainPanel, BorderLayout.CENTER)
    }

    private fun createCommentsPanel(comments: ResponseList<PullRequestComment>?): JBPanel<JBPanel<*>> {
        val commentsPanel = JBPanel<JBPanel<*>>(BorderLayout())

        // comment list (scrollable)
        val listPanel = JBPanel<JBPanel<*>>()
        listPanel.layout = BoxLayout(listPanel, BoxLayout.Y_AXIS)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
        comments?.forEach { comment ->
            val item = panel {
                row {
                    label(comment.createdUser.name).bold()
                    label(dateFormat.format(comment.created))
                }
                row {
                    text(comment.content ?: "")
                }
                separator()
            }
            listPanel.add(item)
        }
        commentsPanel.add(JBScrollPane(listPanel), BorderLayout.CENTER)

        // input area (bottom)
        val inputPanel = JBPanel<JBPanel<*>>(BorderLayout())
        val textArea = JTextArea(3, 40)
        textArea.lineWrap = true
        textArea.wrapStyleWord = true
        inputPanel.add(JBScrollPane(textArea), BorderLayout.CENTER)

        val postButton = JButton(BacklogBundle.message("commentPostButton"))
        postButton.addActionListener {
            val content = textArea.text.trim()
            if (content.isNotEmpty()) {
                postButton.isEnabled = false
                textArea.isEnabled = false
                commentPostListener.onCommentPost(pullRequest, content)
            }
        }
        inputPanel.add(postButton, BorderLayout.EAST)
        commentsPanel.add(inputPanel, BorderLayout.SOUTH)

        return commentsPanel
    }

    fun refreshComments(newComments: ResponseList<PullRequestComment>?) {
        val newPanel = createCommentsPanel(newComments)
        tabbedPane.setComponentAt(3, newPanel)
        tabbedPane.revalidate()
        tabbedPane.repaint()
    }

    private fun createChangesTable(changes: MutableCollection<Change>?, diffSelectionListener: DiffSelectionListener): JBTable {
        val columnNames = arrayOf("File Status", "File Name")
        val data = changes?.map {
            arrayOf(it.fileStatus.toString(), getFileName(it))
        }?.toTypedArray() ?: arrayOf(arrayOf("", ""))

        val tableModel = object : DefaultTableModel(data, columnNames) {
            override fun isCellEditable(row: Int, column: Int): Boolean {
                return false
            }
        }
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

    private fun createCommitsTable(commits: MutableList<GitCommit>?, commitSelectionListener: CommitSelectionListener): JBTable {
        val columnNames = arrayOf("Commit Hash", "Commit Message")
        val data = commits?.map {
            arrayOf(it.id.toShortString(), it.fullMessage)
        }?.toTypedArray() ?: arrayOf(arrayOf("", ""))

        val tableModel = object : DefaultTableModel(data, columnNames) {
            override fun isCellEditable(row: Int, column: Int): Boolean {
                return false
            }
        }
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
        val fullPathStr = when (change.fileStatus) {
            FileStatus.ADDED -> change.afterRevision?.file?.path
            else -> change.beforeRevision?.file?.path
        }
        val fullPath = Path(fullPathStr?:"")

        val relativePath = fullPath.relativeTo(basePath)
        return relativePath.pathString
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

interface CommentPostListener {
    fun onCommentPost(pullRequest: PullRequest, content: String)
}

interface StatusChangeListener {
    fun onStatusChange(pullRequest: PullRequest, newStatus: PullRequest.StatusType)
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