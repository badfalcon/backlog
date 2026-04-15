package com.github.badfalcon.backlog.service

import com.github.badfalcon.backlog.config.MyPluginSettingsState
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

    override fun tearDown() {
        backlogService.backlogClient = null
        backlogService.projectKey = ""
        backlogService.repoId = 0
        backlogService.repoName = ""
        val settings = MyPluginSettingsState.getInstance(project)
        settings.workspaceName = ""
        settings.apiKey = ""
        settings.projectName = ""
        settings.topLevelDomain = BacklogService.TopLevelDomain.COM
        super.tearDown()
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
        every { mockRepo.name } returns "repo"

        val mockProject = mockk<BacklogProject>()
        every { mockProject.projectKey } returns "PROJ"

        val mockPullRequests = mockk<ResponseList<PullRequest>>(relaxed = true)

        val mockClient = mockk<BacklogClient>()
        val mockProjectList = mockk<ResponseList<BacklogProject>>(relaxed = true)
        every { mockProjectList.iterator() } returns mutableListOf(mockProject).iterator()
        every { mockClient.projects } returns mockProjectList

        val mockRepoList = mockk<ResponseList<Repository>>(relaxed = true)
        every { mockRepoList.iterator() } returns mutableListOf(mockRepo).iterator()
        every { mockClient.getGitRepositories("PROJ") } returns mockRepoList
        every { mockClient.getPullRequests(eq("PROJ"), eq(42L), any()) } returns mockPullRequests

        backlogService.backlogClient = mockClient
        val result = backlogService.getPullRequests(targetUrl)

        assertNotNull(result)
        assertEquals("PROJ", backlogService.projectKey)
        assertEquals(42L, backlogService.repoId)
        assertEquals("repo", backlogService.repoName)
    }

    fun testGetPullRequestsNoMatchingRepo() {
        val mockRepo = mockk<Repository>()
        every { mockRepo.httpUrl } returns "https://workspace.backlog.com/git/OTHER/other.git"

        val mockProject = mockk<BacklogProject>()
        every { mockProject.projectKey } returns "OTHER"

        val mockClient = mockk<BacklogClient>()
        val mockProjectList = mockk<ResponseList<BacklogProject>>(relaxed = true)
        every { mockProjectList.iterator() } returns mutableListOf(mockProject).iterator()
        every { mockClient.projects } returns mockProjectList

        val mockRepoList = mockk<ResponseList<Repository>>(relaxed = true)
        every { mockRepoList.iterator() } returns mutableListOf(mockRepo).iterator()
        every { mockClient.getGitRepositories("OTHER") } returns mockRepoList

        backlogService.backlogClient = mockClient
        val result = backlogService.getPullRequests("https://workspace.backlog.com/git/PROJ/repo.git")

        assertNull(result)
    }

    // --- getPullRequestUrl tests ---

    fun testGetPullRequestUrlSuccess() {
        val settings = MyPluginSettingsState.getInstance(project)
        settings.workspaceName = "myworkspace"
        settings.topLevelDomain = BacklogService.TopLevelDomain.COM

        backlogService.projectKey = "PROJ"
        backlogService.repoName = "repo"

        val url = backlogService.getPullRequestUrl(project, 123L)
        assertEquals("https://myworkspace.backlog.com/git/PROJ/repo/pullRequests/123", url)
    }

    fun testGetPullRequestUrlWithJpDomain() {
        val settings = MyPluginSettingsState.getInstance(project)
        settings.workspaceName = "myworkspace"
        settings.topLevelDomain = BacklogService.TopLevelDomain.JP

        backlogService.projectKey = "PROJ"
        backlogService.repoName = "repo"

        val url = backlogService.getPullRequestUrl(project, 456L)
        assertEquals("https://myworkspace.backlog.jp/git/PROJ/repo/pullRequests/456", url)
    }

    fun testGetPullRequestUrlReturnsNullWhenMissingData() {
        val settings = MyPluginSettingsState.getInstance(project)
        settings.workspaceName = ""

        backlogService.projectKey = "PROJ"
        backlogService.repoName = "repo"

        assertNull(backlogService.getPullRequestUrl(project, 123L))
    }

    fun testGetPullRequestUrlReturnsNullWhenMissingRepoName() {
        val settings = MyPluginSettingsState.getInstance(project)
        settings.workspaceName = "myworkspace"

        backlogService.projectKey = "PROJ"
        backlogService.repoName = ""

        assertNull(backlogService.getPullRequestUrl(project, 123L))
    }

    fun testGetPullRequestUrlReturnsNullWhenMissingProjectKey() {
        val settings = MyPluginSettingsState.getInstance(project)
        settings.workspaceName = "myworkspace"

        backlogService.projectKey = ""
        backlogService.repoName = "repo"

        assertNull(backlogService.getPullRequestUrl(project, 123L))
    }

    // --- getImageAttachments tests ---

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
