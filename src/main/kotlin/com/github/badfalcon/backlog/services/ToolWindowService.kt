package com.github.badfalcon.backlog.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.swing.JComponent
import javax.swing.JPanel

@Service(Service.Level.PROJECT)
class ToolWindowService(private var project: Project, private val cs: CoroutineScope) {
    var pullRequestService: PullRequestService? = null

    init {
        pullRequestService = project.service<PullRequestService>()
    }

    fun getPullRequests() {
        cs.launch {
            var pullRequests = pullRequestService?.getPullRequests()
        }
    }

    fun createHomeTabContent(): JComponent {
        var p = panel {
            row {
                label("Home")
            }
        }
        return JBScrollPane(p)
    }

    fun createPullRequestTabContent(): JComponent {
        var p = panel {
            row {
                label("Pull Request")
            }
        }
        return JBScrollPane(p)
    }
}
