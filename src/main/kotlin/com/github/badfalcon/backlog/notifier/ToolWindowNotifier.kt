package com.github.badfalcon.backlog.notifier

import com.intellij.util.messages.Topic

interface ToolWindowNotifier {
    companion object {
        val UPDATE_TOPIC = Topic.create("ToolWindowNotifier", ToolWindowNotifier::class.java)
    }

    fun update(message: String)
}