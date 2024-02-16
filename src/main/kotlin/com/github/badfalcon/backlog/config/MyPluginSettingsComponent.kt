package com.github.badfalcon.backlog.config

import com.github.badfalcon.backlog.services.BacklogService
import com.github.badfalcon.backlog.services.MyProjectService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.nulabinc.backlog4j.BacklogClient
import com.nulabinc.backlog4j.BacklogClientBase
import com.nulabinc.backlog4j.BacklogClientFactory
import com.nulabinc.backlog4j.PullRequest.StatusType
import com.nulabinc.backlog4j.api.option.PullRequestQueryParams
import com.nulabinc.backlog4j.conf.BacklogConfigure
import com.nulabinc.backlog4j.conf.BacklogJpConfigure
import com.nulabinc.backlog4j.http.BacklogHttpClient
//import com.nulabinc.backlog4j.http.httpclient.HttpClientBacklogHttpClient
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel


/**
 * Supports creating and managing a [JPanel] for the Settings Dialog.
 */
class MyPluginSettingsComponent {
    val panel: JPanel
    private val myWorkspaceNameText = JBTextField()
    private val myApiKeyText = JBTextField()
    private val myProjectNameText = JBTextField()

    private val myButtonPanel = JPanel()
    private val myInputCheckButton = JButton("Check Status")
    private val myInputStatusCheckLabel = JBLabel()

    init {
        setupButtonAction()

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Enter workspace name: "), myWorkspaceNameText, 1, false)
            .addLabeledComponent(JBLabel("Enter api key: "), myApiKeyText, 1, false)
            .addLabeledComponent(JBLabel("(option)Enter project name:"), myProjectNameText, 1, false)
            .addComponent(myButtonPanel, 1)  // ボタンとステータスラベルを含むパネルを追加します
            .addComponentFillVertically(JPanel(), 0).panel

        // テストボタンのレイアウト設定(ボタンとラベルの横並びのため)
        myButtonPanel.layout = BoxLayout(myButtonPanel, BoxLayout.X_AXIS)  // パネルを横並びに配置するためにBoxLayoutを使用します
        myButtonPanel.add(myInputCheckButton)
        myButtonPanel.add(myInputStatusCheckLabel)
        myInputStatusCheckLabel.isVisible = false  // 初期状態では非表示にします
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

    private fun setupButtonAction() {
        myInputCheckButton.addActionListener {
            if(workspaceNameText != "" && apiKeyText != ""){
                // check if the values are valid
                val isValid : Boolean = service<BacklogService>().isValidBacklogConfigs(workspaceNameText, apiKeyText)
                updateStatus(isValid);
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