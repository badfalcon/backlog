package com.github.badfalcon.backlog.service

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.nulabinc.backlog4j.*
import com.nulabinc.backlog4j.Project as BacklogProject
import io.mockk.every
import io.mockk.mockk

class BacklogServiceTest : BasePlatformTestCase() {

    private lateinit var backlogService: BacklogService

    override fun setUp() {
        super.setUp()
        backlogService = project.getService(BacklogService::class.java)
    }

    // --- TopLevelDomain tests ---

    fun testTopLevelDomainValues() {
        assertEquals("com", BacklogService.TopLevelDomain.COM.value)
        assertEquals("jp", BacklogService.TopLevelDomain.JP.value)
    }

    // --- Guard condition tests ---

    fun testIsReadyDefaultFalse() {
        assertFalse(backlogService.isReady)
    }

    fun testValidateEmptyWorkspaceThrows() {
        try {
            backlogService.validateBacklogConfigs("", "some-key", BacklogService.TopLevelDomain.COM)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    fun testValidateEmptyApiKeyThrows() {
        try {
            backlogService.validateBacklogConfigs("workspace", "", BacklogService.TopLevelDomain.COM)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    fun testIsValidReturnsNullOnException() {
        val result = backlogService.isValidBacklogConfigs("invalid", "invalid", BacklogService.TopLevelDomain.COM)
        assertNull(result)
    }

    fun testGetPullRequestsWhenNotReady() {
        val result = backlogService.getPullRequests("https://example.backlog.com/git/PROJ/repo.git")
        assertNull(result)
    }

    fun testGetImageAttachmentsWhenNotReady() {
        val result = backlogService.getImageAttachments(1L, mutableListOf())
        assertTrue(result.isEmpty())
    }

    // --- Ready state tests ---

    fun testIsReadyTrueWhenClientSet() {
        val mockClient = mockk<BacklogClient>(relaxed = true)
        backlogService.backlogClient = mockClient
        assertTrue(backlogService.isReady)
    }

    fun testGetPullRequestsFindsMatchingRepo() {
        val targetUrl = "https://workspace.backlog.com/git/PROJ/repo.git"

        val mockRepo = mockk<Repository>()
        every { mockRepo.httpUrl } returns targetUrl
        every { mockRepo.id } returns 42L

        val mockProject = mockk<BacklogProject>()
        every { mockProject.projectKey } returns "PROJ"

        val mockPullRequests = mockk<ResponseList<PullRequest>>(relaxed = true)

        val mockClient = mockk<BacklogClient>()
        every { mockClient.projects } returns listOf(mockProject) as ResponseList<BacklogProject>

        // Use any() matcher for more flexible matching
        every { mockClient.getGitRepositories("PROJ") } returns mockk {
            val repoList = listOf(mockRepo)
            every { iterator() } returns repoList.iterator()
            every { size } returns 1
        }
        every { mockClient.getPullRequests(eq("PROJ"), eq(42L), any()) } returns mockPullRequests

        backlogService.backlogClient = mockClient
        val result = backlogService.getPullRequests(targetUrl)

        assertNotNull(result)
        assertEquals("PROJ", backlogService.projectKey)
        assertEquals(42L, backlogService.repoId)
    }

    fun testGetPullRequestsNoMatchingRepo() {
        val mockRepo = mockk<Repository>()
        every { mockRepo.httpUrl } returns "https://workspace.backlog.com/git/OTHER/other.git"

        val mockProject = mockk<BacklogProject>()
        every { mockProject.projectKey } returns "OTHER"

        val mockClient = mockk<BacklogClient>()
        every { mockClient.projects } returns listOf(mockProject) as ResponseList<BacklogProject>
        every { mockClient.getGitRepositories("OTHER") } returns mockk {
            val repoList = listOf(mockRepo)
            every { iterator() } returns repoList.iterator()
            every { size } returns 1
        }

        backlogService.backlogClient = mockClient
        val result = backlogService.getPullRequests("https://workspace.backlog.com/git/PROJ/repo.git")

        assertNull(result)
    }

    fun testGetImageAttachmentsWhenReady() {
        val mockAttachmentData = mockk<AttachmentData>()
        val mockAttachment = mockk<Attachment>()
        every { mockAttachment.id } returns 10L

        val mockClient = mockk<BacklogClient>()
        every { mockClient.downloadPullRequestAttachment(any(), any(), any(), eq(10L)) } returns mockAttachmentData

        backlogService.backlogClient = mockClient
        backlogService.projectKey = "PROJ"
        backlogService.repoId = 42L

        val result = backlogService.getImageAttachments(1L, mutableListOf(mockAttachment))
        assertEquals(1, result.size)
        assertSame(mockAttachmentData, result[0])
    }
}
