package com.github.badfalcon.backlog.service

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class BacklogUrlRegexTest : BasePlatformTestCase() {

    private val regex = GitService.BACKLOG_URL_REGEX

    fun testBacklogComUrl() {
        assertTrue(regex.containsMatchIn("https://workspace.backlog.com/git/PROJ/repo.git"))
    }

    fun testBacklogJpUrl() {
        assertTrue(regex.containsMatchIn("https://workspace.backlog.jp/git/PROJ/repo.git"))
    }

    fun testGithubUrl() {
        assertFalse(regex.containsMatchIn("https://github.com/user/repo.git"))
    }

    fun testHttpUrl() {
        assertFalse(regex.containsMatchIn("http://workspace.backlog.com/git/PROJ/repo.git"))
    }

    fun testSshUrl() {
        assertFalse(regex.containsMatchIn("git@workspace.backlog.com:PROJ/repo.git"))
    }

    fun testNoGitSuffix() {
        assertFalse(regex.containsMatchIn("https://workspace.backlog.com/git/PROJ/repo"))
    }

    fun testBacklogOrgUrl() {
        assertFalse(regex.containsMatchIn("https://workspace.backlog.org/git/PROJ/repo.git"))
    }

    fun testEmbeddedBacklogUrlPartialMatch() {
        // containsMatchIn will match embedded URLs — this documents the current behavior
        assertTrue(regex.containsMatchIn("https://evil.com?redirect=https://ws.backlog.com/git/P/r.git"))
    }
}
