package com.github.badfalcon.backlog.config

import com.github.badfalcon.backlog.notifier.UPDATE_TOPIC
import com.github.badfalcon.backlog.service.BacklogService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel


/**
 * Supports creating and managing a [JPanel] for the Settings Dialog.
 */
class MyPluginSettingsComponent(private var project: Project) {
    val panel: DialogPanel
    private val myWorkspaceNameText = JBTextField()
    private val myApiKeyText = JBTextField()
    private val myProjectNameText = JBTextField()
    private val comboBox = ComboBox(BacklogService.TopLevelDomain.entries.map { it.value }.toTypedArray())

    private val myInputCheckButton = JButton("run")
    private val myInputStatusCheckLabel = JBLabel()

    init {
        setupButtonAction()
        panel = panel {
            row {
                cell(JBLabel("Backlog")).bold()
            }
            group("Backlog Settings") {
                row("workspace info: ") {
                    cell (JBLabel("https://")).gap(RightGap.SMALL)
                    cell(myWorkspaceNameText).gap(RightGap.SMALL)
                    cell (JBLabel(".backlog.")).gap(RightGap.SMALL)
                    cell (comboBox)
                }.layout(RowLayout.LABEL_ALIGNED)
                row("api key: ") {
                    cell(myApiKeyText).resizableColumn().align(AlignX.FILL)
                }.layout(RowLayout.LABEL_ALIGNED)
                row("project name: ") {
                    cell(myProjectNameText).resizableColumn().align(AlignX.FILL)
                }.layout(RowLayout.LABEL_ALIGNED)
                row("Check Valid: ") {
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
        get() = myApiKeyText.text
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
            return BacklogService.TopLevelDomain.entries.first { it.value == selectedValue }
        }
        set(newSelection) {
            comboBox.selectedItem = newSelection.value
        }

    private fun setupButtonAction() {
        myInputCheckButton.addActionListener {
            if(workspaceNameText != "" && apiKeyText != ""){
                // check if the values are valid
                val backlogService = project.service<BacklogService>()
                val config = backlogService.isValidBacklogConfigs(workspaceNameText, apiKeyText, topLevelDomain)
                val isValid : Boolean = config != null

                updateStatus(isValid)
                if ( isValid){

                    val messageBus = project.messageBus
                    val publisher = messageBus.syncPublisher(UPDATE_TOPIC)
                    publisher.update("button Action")
                }
            }
        }
    }

    private fun updateStatus(success: Boolean) {
        if (success) {
            myInputStatusCheckLabel.text = "Success"
            myInputStatusCheckLabel.foreground = JBColor.GREEN
        } else {
            myInputStatusCheckLabel.text = "Failure"
            myInputStatusCheckLabel.foreground = JBColor.RED
        }
        myInputStatusCheckLabel.isVisible = true
        // todo set the status text disappear by time
    }
}