package com.github.badfalcon.backlog.service

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import io.mockk.every
import io.mockk.mockk

class GitServiceTest : BasePlatformTestCase() {

    private lateinit var gitService: GitService

    override fun setUp() {
        super.setUp()
        gitService = project.getService(GitService::class.java)
    }

    override fun tearDown() {
        gitService.repository = null
        super.tearDown()
    }

    // --- Guard condition tests ---

    fun testIsReadyDefaultFalse() {
        assertFalse(gitService.isReady)
    }

    fun testGetRemoteUrlWhenNotReady() {
        val result = gitService.getRemoteUrl()
        assertNull(result)
    }

    fun testGetChangesWhenNotReady() {
        val result = gitService.getChanges("main", "feature")
        assertNull(result)
    }

    fun testGetCommitsWhenNotReady() {
        val result = gitService.getCommits("main", "feature")
        assertNull(result)
    }

    // --- Happy path tests with mocked repository ---

    fun testGetRemoteUrlMatchesBacklogUrl() {
        val backlogUrl = "https://workspace.backlog.com/git/PROJ/repo.git"
        val mockRemote = mockk<GitRemote>(relaxed = true)
        every { mockRemote.firstUrl } returns backlogUrl

        val mockRepository = mockk<GitRepository>(relaxed = true)
        every { mockRepository.remotes } returns listOf(mockRemote)

        gitService.repository = mockRepository

        val result = gitService.getRemoteUrl()
        assertEquals(backlogUrl, result)
    }

    fun testGetRemoteUrlSkipsNonBacklogUrl() {
        val githubUrl = "https://github.com/user/repo.git"
        val mockRemote = mockk<GitRemote>(relaxed = true)
        every { mockRemote.firstUrl } returns githubUrl

        val mockRepository = mockk<GitRepository>(relaxed = true)
        every { mockRepository.remotes } returns listOf(mockRemote)

        gitService.repository = mockRepository

        val result = gitService.getRemoteUrl()
        assertNull(result)
    }
}
