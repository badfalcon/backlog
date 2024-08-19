package com.github.badfalcon.backlog.notifier

import com.intellij.util.messages.Topic

interface ToolWindowNotifier {
    fun update(message: String)
}

@Topic.ProjectLevel
val UPDATE_TOPIC = Topic.create("ToolWindowNotifier", ToolWindowNotifier::class.java)
