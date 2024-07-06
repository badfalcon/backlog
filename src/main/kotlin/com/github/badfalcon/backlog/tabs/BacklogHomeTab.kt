package com.github.badfalcon.backlog.tabs

import com.github.badfalcon.backlog.notifier.UPDATE_TOPIC
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.nulabinc.backlog4j.PullRequest
import com.nulabinc.backlog4j.ResponseList
import net.miginfocom.swing.MigLayout
import javax.swing.JButton

class BacklogHomeTab(private val pullRequestSelectionListener: PullRequestSelectionListener) : JBPanel<JBPanel<*>>(){

val reloadButton: JButton = JButton("reload")
    val statusLabel: JBLabel = JBLabel()

    var pullRequestList: JBList<String?>? = null

    init {
        thisLogger().warn("[backlog] "+ "BacklogHomeTab.init")
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

        reload()
    }

    fun getContent() = this

    fun reload() {
        thisLogger().warn("[backlog] "+ "BacklogHomeTab.reload")
        removeAll()
        layout = MigLayout()
        add(reloadButton, "center")
        add(statusLabel, "wrap, center")
        if (pullRequestList != null) {
            add(pullRequestList, "grow, center")
        }
        // update status label 3 seconds after reload
        Thread {
            Thread.sleep(3000)
            statusLabel.text = ""
        }.start()
    }

    fun update(pullRequests: ResponseList<PullRequest>?) {
        thisLogger().warn("[backlog] "+ "BacklogHomeTab.update")
        if (pullRequests != null) {
            apply {
                thisLogger().warn("[BLPL]getContent")
                removeAll()
                println("[BLPL] " + "gitRepo")

                var p = pullRequests.map { it.summary }
                pullRequestList = JBList(p)

                // プルリクエストが選択されたときのリスナーを設定
                pullRequestList!!.addListSelectionListener { e ->
                    if (!e.valueIsAdjusting) {
                        // todo create a new tab
                        pullRequestSelectionListener.onPullRequestSelected(pullRequests[pullRequestList!!.selectedIndex])
                    }
                }

                // pullRequestList をパネルに追加
                add(pullRequestList)
            }
        }
        reload()
    }
}

interface PullRequestSelectionListener {
    fun onPullRequestSelected(pullRequest: PullRequest)
}