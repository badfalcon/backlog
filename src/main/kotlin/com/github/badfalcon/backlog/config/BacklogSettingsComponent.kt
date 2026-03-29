package com.github.badfalcon.backlog.config

import com.github.badfalcon.backlog.BacklogBundle
import com.github.badfalcon.backlog.notifier.UPDATE_TOPIC
import com.github.badfalcon.backlog.service.BacklogService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.Timer


/**
 * Supports creating and managing a [JPanel] for the Settings Dialog.
 */
class BacklogSettingsComponent(private val project: Project) {
    val panel: DialogPanel
    private val myWorkspaceNameText = JBTextField()
    private val myApiKeyText = JBPasswordField()
    private val myProjectNameText = JBTextField()
    private val comboBox = ComboBox(BacklogService.TopLevelDomain.entries.map { it.value }.toTypedArray())

    private val myInputCheckButton = JButton(BacklogBundle.message("settings.validate.button"))
    private val myInputStatusCheckLabel = JBLabel()
    private var statusDismissTimer: Timer? = null

    init {
        setupButtonAction()
        panel = panel {
            row {
                cell(JBLabel("Backlog")).bold()
            }
            group(BacklogBundle.message("settings.group.title")) {
                row(BacklogBundle.message("settings.workspace.label")) {
                    @Suppress("DialogTitleCapitalization")
                    cell(JBLabel("https://")).gap(RightGap.SMALL)
                    cell(myWorkspaceNameText).gap(RightGap.SMALL)
                    cell(JBLabel(".backlog.")).gap(RightGap.SMALL)
                    cell(comboBox)
                }.layout(RowLayout.LABEL_ALIGNED)
                row(BacklogBundle.message("settings.apiKey.label")) {
                    cell(myApiKeyText).resizableColumn().align(AlignX.FILL)
                }.layout(RowLayout.LABEL_ALIGNED)
                row(BacklogBundle.message("settings.projectName.label")) {
                    cell(myProjectNameText).resizableColumn().align(AlignX.FILL)
                }.layout(RowLayout.LABEL_ALIGNED)
                row(BacklogBundle.message("settings.validate.label")) {
                    cell(myInputCheckButton)
                    cell(myInputStatusCheckLabel)
                }.layout(RowLayout.LABEL_ALIGNED)
            }
        }

        myInputStatusCheckLabel.isVisible = false
    }

    val preferredFocusedComponent: JComponent
        get() = myWorkspaceNameText

    var workspaceNameText: String
        get() = myWorkspaceNameText.text
        set(newText) {
            myWorkspaceNameText.text = newText
        }

    var apiKeyText: String
        get() = String(myApiKeyText.password)
        set(newText) {
            myApiKeyText.text = newText
        }

    var projectNameText: String
        get() = myProjectNameText.text
        set(newText) {
            myProjectNameText.text = newText
        }

    var topLevelDomain: BacklogService.TopLevelDomain
        get() {
            val selectedValue = comboBox.selectedItem as String
            return BacklogService.TopLevelDomain.entries.firstOrNull { it.value == selectedValue }
                ?: BacklogService.TopLevelDomain.COM
        }
        set(newSelection) {
            comboBox.selectedItem = newSelection.value
        }

    private fun setupButtonAction() {
        myInputCheckButton.addActionListener {
            if (workspaceNameText.isNotEmpty() && apiKeyText.isNotEmpty()) {
                myInputCheckButton.isEnabled = false
                myInputStatusCheckLabel.text = BacklogBundle.message("settings.validation.checking")
                myInputStatusCheckLabel.foreground = JBColor.foreground()
                myInputStatusCheckLabel.isVisible = true

                val backlogService = project.getService(BacklogService::class.java)
                val workspace = workspaceNameText
                val apiKey = apiKeyText
                val tld = topLevelDomain

                ApplicationManager.getApplication().executeOnPooledThread {
                    try {
                        backlogService.validateBacklogConfigs(workspace, apiKey, tld)
                        ApplicationManager.getApplication().invokeLater {
                            updateStatus(true)
                            val messageBus = project.messageBus
                            val publisher = messageBus.syncPublisher(UPDATE_TOPIC)
                            publisher.update("button Action")
                        }
                    } catch (e: Exception) {
                        ApplicationManager.getApplication().invokeLater {
                            updateStatus(false, e.message)
                        }
                    }
                }
            }
        }
    }

    private fun updateStatus(success: Boolean, errorMessage: String? = null) {
        myInputCheckButton.isEnabled = true
        if (success) {
            myInputStatusCheckLabel.text = BacklogBundle.message("settings.validation.success")
            myInputStatusCheckLabel.foreground = JBColor.GREEN
        } else {
            val detail = errorMessage ?: BacklogBundle.message("error.unknown")
            myInputStatusCheckLabel.text = BacklogBundle.message("settings.validation.failure", detail)
            myInputStatusCheckLabel.foreground = JBColor.RED
        }
        myInputStatusCheckLabel.isVisible = true

        statusDismissTimer?.stop()
        statusDismissTimer = Timer(5000) {
            myInputStatusCheckLabel.isVisible = false
            myInputStatusCheckLabel.text = ""
        }.apply {
            isRepeats = false
            start()
        }
    }

    fun dispose() {
        statusDismissTimer?.stop()
    }
}