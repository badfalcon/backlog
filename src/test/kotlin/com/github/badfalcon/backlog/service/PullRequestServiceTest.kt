package com.github.badfalcon.backlog.service

import com.intellij.openapi.vcs.changes.Change
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.nulabinc.backlog4j.AttachmentData
import com.nulabinc.backlog4j.PullRequest
import com.nulabinc.backlog4j.ResponseList
import git4idea.GitCommit
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class PullRequestServiceTest : BasePlatformTestCase() {

    private lateinit var pullRequestService: PullRequestService

    override fun setUp() {
        super.setUp()
        pullRequestService = project.getService(PullRequestService::class.java)
        // Reset to real services to prevent state leaking between tests
        pullRequestService.backlogService = project.getService(BacklogService::class.java)
        pullRequestService.gitService = project.getService(GitService::class.java)
    }

    // --- Guard condition tests (default state: both services not ready) ---

    fun testGetPullRequestsWhenNotReady() {
        val result = pullRequestService.getPullRequests()
        assertNull(result)
    }

    fun testGetChangesWhenNotReady() {
        val mockPR = mockk<PullRequest>()
        every { mockPR.base } returns "main"
        every { mockPR.branch } returns "feature"

        val result = pullRequestService.getChanges(mockPR)
        assertNull(result)
    }

    fun testGetCommitsWhenNotReady() {
        val mockPR = mockk<PullRequest>()
        every { mockPR.base } returns "main"
        every { mockPR.branch } returns "feature"

        val result = pullRequestService.getCommits(mockPR)
        assertNull(result)
    }

    // --- Happy path tests (mock services) ---

    fun testGetPullRequestsWhenReady() {
        val mockPRList = mockk<ResponseList<PullRequest>>(relaxed = true)
        val mockBacklogService = mockk<BacklogService>()
        every { mockBacklogService.isReady } returns true
        every { mockBacklogService.getPullRequests(any()) } returns mockPRList

        val mockGitService = mockk<GitService>()
        every { mockGitService.isReady } returns true
        every { mockGitService.fetch() } returns Unit
        every { mockGitService.getRemoteUrl() } returns "https://ws.backlog.com/git/PROJ/repo.git"

        pullRequestService.backlogService = mockBacklogService
        pullRequestService.gitService = mockGitService

        val result = pullRequestService.getPullRequests()
        assertNotNull(result)
        verify { mockGitService.fetch() }
        verify { mockGitService.getRemoteUrl() }
        verify { mockBacklogService.getPullRequests("https://ws.backlog.com/git/PROJ/repo.git") }
    }

    fun testGetPullRequestsNullRemoteUrl() {
        val mockBacklogService = mockk<BacklogService>()
        every { mockBacklogService.isReady } returns true

        val mockGitService = mockk<GitService>()
        every { mockGitService.isReady } returns true
        every { mockGitService.fetch() } returns Unit
        every { mockGitService.getRemoteUrl() } returns null

        pullRequestService.backlogService = mockBacklogService
        pullRequestService.gitService = mockGitService

        val result = pullRequestService.getPullRequests()
        assertNull(result)
    }

    fun testGetChangesWhenReady() {
        val mockChanges = mutableListOf<Change>()
        val mockGitService = mockk<GitService>()
        every { mockGitService.isReady } returns true
        every { mockGitService.getChanges("main", "feature") } returns mockChanges

        pullRequestService.gitService = mockGitService

        val mockPR = mockk<PullRequest>()
        every { mockPR.base } returns "main"
        every { mockPR.branch } returns "feature"

        val result = pullRequestService.getChanges(mockPR)
        assertSame(mockChanges, result)
    }

    fun testGetCommitsWhenReady() {
        val mockCommits = mutableListOf<GitCommit>()
        val mockGitService = mockk<GitService>()
        every { mockGitService.isReady } returns true
        every { mockGitService.getCommits("main", "feature") } returns mockCommits

        pullRequestService.gitService = mockGitService

        val mockPR = mockk<PullRequest>()
        every { mockPR.base } returns "main"
        every { mockPR.branch } returns "feature"

        val result = pullRequestService.getCommits(mockPR)
        assertSame(mockCommits, result)
    }

    fun testGetAttachments() {
        val mockAttachmentData = mutableListOf<AttachmentData>()
        val mockBacklogService = mockk<BacklogService>()
        every { mockBacklogService.getImageAttachments(any(), any()) } returns mockAttachmentData

        pullRequestService.backlogService = mockBacklogService

        val mockPR = mockk<PullRequest>(relaxed = true)
        every { mockPR.number } returns 123L
        every { mockPR.attachments } returns mutableListOf()

        val result = pullRequestService.getAttachments(mockPR)
        assertSame(mockAttachmentData, result)
    }
}
