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
        assertTrue(result.contains("<body>hello</body>"))
    }

    fun testEmptyString() {
        val result = converter.toHtml("", null, null)
        assertTrue(result.contains("<body></body>"))
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
        assertTrue(result.startsWith("<html>"))
        assertTrue(result.endsWith("</body></html>"))
        assertTrue(result.contains("<body>test</body>"))
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
        assertTrue(result.contains("<img src=\"data:image/jpeg;base64,"))
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

    fun testQuote() {
        val result = converter.toHtml("{quote}quoted text{/quote}", null, null)
        assertTrue(result.contains("<blockquote>quoted text</blockquote>"))
        assertFalse(result.contains("{quote}"))
        assertFalse(result.contains("{/quote}"))
    }

    fun testQuoteMultiline() {
        val result = converter.toHtml("{quote}line1\nline2{/quote}", null, null)
        assertTrue(result.contains("<blockquote>line1<br>line2</blockquote>"))
    }

    fun testMultipleQuotes() {
        val result = converter.toHtml("{quote}first{/quote}\n{quote}second{/quote}", null, null)
        assertTrue(result.contains("<blockquote>first</blockquote>"))
        assertTrue(result.contains("<blockquote>second</blockquote>"))
    }

    fun testCodeBlock() {
        val result = converter.toHtml("{code}val x = 1{/code}", null, null)
        assertTrue(result.contains("<pre><code>val x = 1</code></pre>"))
        assertFalse(result.contains("{code}"))
    }

    fun testCodeBlockMultiline() {
        val result = converter.toHtml("{code}line1\nline2{/code}", null, null)
        assertTrue(result.contains("<pre><code>line1\nline2</code></pre>"))
        assertFalse(result.contains("<pre><code>line1<br>line2</code></pre>"))
    }

    fun testBold() {
        val result = converter.toHtml("'''bold text'''", null, null)
        assertTrue(result.contains("<b>bold text</b>"))
        assertFalse(result.contains("'''"))
    }

    fun testItalic() {
        val result = converter.toHtml("''italic text''", null, null)
        assertTrue(result.contains("<i>italic text</i>"))
        assertFalse(result.contains("''"))
    }

    fun testStrikethrough() {
        val result = converter.toHtml("%%deleted%%", null, null)
        assertTrue(result.contains("<s>deleted</s>"))
        assertFalse(result.contains("%%"))
    }

    fun testLink() {
        val result = converter.toHtml("[[Google>https://google.com]]", null, null)
        assertTrue(result.contains("<a href=\"https://google.com\">Google</a>"))
        assertFalse(result.contains("[["))
    }

    fun testColor() {
        val result = converter.toHtml("&color(red){warning}&", null, null)
        assertTrue(result.contains("<span style=\"color:red\">warning</span>"))
        assertFalse(result.contains("&color"))
    }

    fun testBareUrl() {
        val result = converter.toHtml("see https://example.com for details", null, null)
        assertTrue(result.contains("<a href=\"https://example.com\">https://example.com</a>"))
    }

    fun testBareUrlNotDoubled() {
        val result = converter.toHtml("[[link>https://example.com]]", null, null)
        assertEquals(1, Regex("""<a """).findAll(result).count())
    }

    fun testBareHttpUrl() {
        val result = converter.toHtml("http://example.com", null, null)
        assertTrue(result.contains("<a href=\"http://example.com\">http://example.com</a>"))
    }

    fun testCodeBlockPreservesMarkup() {
        val result = converter.toHtml("{code}'''not bold''' and ''not italic''{/code}", null, null)
        assertTrue(result.contains("'''not bold'''"))
        assertFalse(result.contains("<b>"))
        assertFalse(result.contains("<i>"))
    }

    fun testBareUrlExcludesJapanesePunctuation() {
        val result = converter.toHtml("see https://example.com。details", null, null)
        assertTrue(result.contains("<a href=\"https://example.com\">https://example.com</a>"))
        assertTrue(result.contains("。details"))
    }

    fun testBoldAndItalicCombined() {
        val result = converter.toHtml("'''bold''' and ''italic''", null, null)
        assertTrue(result.contains("<b>bold</b>"))
        assertTrue(result.contains("<i>italic</i>"))
    }

    fun testTable() {
        val result = converter.toHtml("{table}col1|col2\nval1|val2{/table}", null, null)
        assertTrue(result.contains("<table>"))
        assertTrue(result.contains("<th>col1</th><th>col2</th>"))
        assertTrue(result.contains("<td>val1</td><td>val2</td>"))
        assertTrue(result.contains("</table>"))
        assertFalse(result.contains("{table}"))
    }

    fun testTableSingleRow() {
        val result = converter.toHtml("{table}a|b{/table}", null, null)
        assertTrue(result.contains("<th>a</th><th>b</th>"))
    }

    fun testTableMultipleRows() {
        val result = converter.toHtml("{table}h1|h2\nr1c1|r1c2\nr2c1|r2c2{/table}", null, null)
        assertTrue(result.contains("<th>h1</th><th>h2</th>"))
        assertTrue(result.contains("<td>r1c1</td><td>r1c2</td>"))
        assertTrue(result.contains("<td>r2c1</td><td>r2c2</td>"))
    }

    fun testThumbnail() {
        val imageBytes = "fake-image-data".toByteArray()

        val attachment = mockk<Attachment>()
        every { attachment.name } returns "thumb.png"
        every { attachment.isImage } returns true

        val attachmentData = mockk<AttachmentData>()
        every { attachmentData.filename } returns "thumb.png"
        every { attachmentData.content } returns ByteArrayInputStream(imageBytes)

        val result = converter.toHtml(
            "#thumbnail(thumb.png)",
            mutableListOf(attachment),
            mutableListOf(attachmentData)
        )
        assertTrue(result.contains("<img src=\"data:image/jpeg;base64,"))
        assertTrue(result.contains("class=\"thumbnail\""))
        assertFalse(result.contains("#thumbnail"))
    }

    fun testCombinedMarkup() {
        val result = converter.toHtml("* Header\n- item1\n- item2\nsome text\n", null, null)
        assertTrue(result.contains("<h1>Header</h1>"))
        assertTrue(result.contains("<li>item1</li>"))
        assertTrue(result.contains("<li>item2</li>"))
        assertTrue(result.contains("<br>"))
    }
}
