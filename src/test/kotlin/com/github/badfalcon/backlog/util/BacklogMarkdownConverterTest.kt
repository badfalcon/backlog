package com.github.badfalcon.backlog.util

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.nulabinc.backlog4j.Attachment
import com.nulabinc.backlog4j.AttachmentData
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayInputStream

class BacklogMarkdownConverterTest : BasePlatformTestCase() {

    private val converter = BacklogMarkdownConverter()

    fun testPlainText() {
        val result = converter.toHtml("hello", null, null)
        assertEquals("<html><body>hello</body></html>", result)
    }

    fun testEmptyString() {
        val result = converter.toHtml("", null, null)
        assertEquals("<html><body></body></html>", result)
    }

    fun testSingleHeader() {
        val result = converter.toHtml("* title\n", null, null)
        assertTrue(result.contains("<h1>title</h1>"))
    }

    fun testMultipleHeaders() {
        val result = converter.toHtml("* h1\n** h2\n*** h3\n", null, null)
        assertTrue(result.contains("<h1>h1</h1>"))
        assertTrue(result.contains("<h2>h2</h2>"))
        assertTrue(result.contains("<h3>h3</h3>"))
    }

    fun testHeaderWithoutTrailingNewline() {
        val result = converter.toHtml("* title", null, null)
        assertFalse(result.contains("<h1>"))
    }

    fun testSimpleList() {
        val result = converter.toHtml("- item1\n- item2\n", null, null)
        assertTrue(result.contains("<ul>"))
        assertTrue(result.contains("<li>item1</li>"))
        assertTrue(result.contains("<li>item2</li>"))
    }

    fun testNestedList() {
        val result = converter.toHtml("- parent\n-- child\n", null, null)
        assertTrue(result.contains("<li>parent</li>"))
        assertTrue(result.contains("<li>child</li>"))
    }

    fun testNewlineReplacement() {
        val result = converter.toHtml("a\nb", null, null)
        assertTrue(result.contains("a<br>b"))
    }

    fun testHtmlWrapping() {
        val result = converter.toHtml("test", null, null)
        assertTrue(result.startsWith("<html><body>"))
        assertTrue(result.endsWith("</body></html>"))
    }

    fun testImageReplacement() {
        val imageBytes = "fake-image-data".toByteArray()

        val attachment = mockk<Attachment>()
        every { attachment.name } returns "test.png"
        every { attachment.isImage } returns true

        val attachmentData = mockk<AttachmentData>()
        every { attachmentData.filename } returns "test.png"
        every { attachmentData.content } returns ByteArrayInputStream(imageBytes)

        val result = converter.toHtml(
            "#image(test.png)",
            mutableListOf(attachment),
            mutableListOf(attachmentData)
        )
        assertTrue(result.contains("<img src=\"data:image/png;base64,"))
    }

    fun testNullAttachments() {
        val result = converter.toHtml("#image(test.png)", null, null)
        assertFalse(result.contains("<img"))
        assertTrue(result.contains("#image(test.png)"))
    }

    fun testNonImageSkipped() {
        val attachment = mockk<Attachment>()
        every { attachment.name } returns "doc.pdf"
        every { attachment.isImage } returns false

        val result = converter.toHtml(
            "#image(doc.pdf)",
            mutableListOf(attachment),
            null
        )
        assertFalse(result.contains("<img"))
    }

    fun testImageNotInText() {
        val attachment = mockk<Attachment>()
        every { attachment.name } returns "test.png"
        every { attachment.isImage } returns true

        val attachmentData = mockk<AttachmentData>()
        every { attachmentData.filename } returns "test.png"
        every { attachmentData.content } returns ByteArrayInputStream("data".toByteArray())

        val result = converter.toHtml(
            "no image reference here",
            mutableListOf(attachment),
            mutableListOf(attachmentData)
        )
        assertFalse(result.contains("<img"))
    }

    fun testCombinedMarkup() {
        val result = converter.toHtml("* Header\n- item1\n- item2\nsome text\n", null, null)
        assertTrue(result.contains("<h1>Header</h1>"))
        assertTrue(result.contains("<li>item1</li>"))
        assertTrue(result.contains("<li>item2</li>"))
        assertTrue(result.contains("<br>"))
    }
}
