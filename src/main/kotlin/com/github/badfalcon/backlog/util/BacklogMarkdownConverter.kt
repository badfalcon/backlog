package com.github.badfalcon.backlog.util

import com.nulabinc.backlog4j.Attachment
import com.nulabinc.backlog4j.AttachmentData
import org.apache.commons.codec.binary.Base64

class BacklogMarkdownConverter {

    fun toHtml(src: String, attachments: MutableList<Attachment>?, attachmentData: MutableList<AttachmentData>?): String {
        var result = src

        // extract code blocks first to protect their content from other transformations
        val codeBlocks = mutableListOf<String>()
        val codePattern = Regex("""\{code\}(.*?)\{/code\}""", RegexOption.DOT_MATCHES_ALL)
        result = codePattern.replace(result) { match ->
            val index = codeBlocks.size
            codeBlocks.add(match.groupValues[1])
            "\u0000CODE_BLOCK_$index\u0000"
        }

        // replace headers
        for (i in 9 downTo 1) {
            val headerPattern = Regex("""\*{$i}\s?(.*)\r?\n""")
            val headerReplacement = "<h${i}>$1</h${i}>\n"
            result = headerPattern.replace(result, headerReplacement)
        }

        // replace lists
        for(i in 1 .. 9)
        {
            // replace list groups
            val listGroupPattern = Regex("""(-{$i,}\s.+\r?\n)+""")
            val listGroupReplacement = "<ul>\n$0</ul>\n"
            result = listGroupPattern.replace(result, listGroupReplacement)

            // replace list items
            val listItemPattern = Regex("""^-{$i}\s(.+)$""", RegexOption.MULTILINE)
            val listItemReplacement = "<li>$1</li>"
            result = listItemPattern.replace(result, listItemReplacement)
        }

        // replace quotes
        val quotePattern = Regex("""\{quote\}(.*?)\{/quote\}""", RegexOption.DOT_MATCHES_ALL)
        result = quotePattern.replace(result, "<blockquote>$1</blockquote>")

        // replace bold
        val boldPattern = Regex("""'''(.+?)'''""")
        result = boldPattern.replace(result, "<b>$1</b>")

        // replace italic
        val italicPattern = Regex("""''(.+?)''""")
        result = italicPattern.replace(result, "<i>$1</i>")

        // replace strikethrough
        val strikethroughPattern = Regex("""%%(.+?)%%""")
        result = strikethroughPattern.replace(result, "<s>$1</s>")

        // replace links
        val linkPattern = Regex("""\[\[(.+?)>(.+?)]]""")
        result = linkPattern.replace(result, """<a href="$2">$1</a>""")

        // replace color
        val colorPattern = Regex("""&color\((.+?)\)\{(.+?)\}&""")
        result = colorPattern.replace(result, """<span style="color:$1">$2</span>""")

        // auto-link bare URLs (skip URLs already inside href="...")
        val urlPattern = Regex("""(?<!href=")(?<!\w)(https?://[^\s<>。、）」』】\)]+)""")
        result = urlPattern.replace(result, """<a href="$1">$1</a>""")

        // replace tables
        val tablePattern = Regex("""\{table\}(.*?)\{/table\}""", RegexOption.DOT_MATCHES_ALL)
        result = tablePattern.replace(result) { match ->
            val tableContent = match.groupValues[1].trim()
            val rows = tableContent.split(Regex("""\r?\n"""))
            val htmlRows = rows.mapIndexed { index, row ->
                val cells = row.split("|")
                val tag = if (index == 0) "th" else "td"
                val htmlCells = cells.joinToString("") { "<$tag>${it.trim()}</$tag>" }
                "<tr>$htmlCells</tr>"
            }
            "<table>${htmlRows.joinToString("")}</table>"
        }

        // show images and thumbnails
        if (attachments != null) {
            for (attachment in attachments) {
                if (!attachment.isImage){
                    continue
                }

                val imagePattern = Regex("""#image\(${Regex.escape(attachment.name)}\)""")
                val thumbnailPattern = Regex("""#thumbnail\(${Regex.escape(attachment.name)}\)""")
                val hasImage = imagePattern.containsMatchIn(result)
                val hasThumbnail = thumbnailPattern.containsMatchIn(result)

                if (!hasImage && !hasThumbnail) {
                    continue
                }

                val attachmentDatum = attachmentData?.find { it.filename == attachment.name } ?: continue
                val bytes = attachmentDatum.content.readBytes()
                val base64 = Base64.encodeBase64String(bytes)

                if (hasImage) {
                    val imageReplacement = "<img src=\"data:image/jpeg;base64,${base64}\">"
                    result = imagePattern.replace(result, imageReplacement)
                }
                if (hasThumbnail) {
                    val thumbnailReplacement = "<img src=\"data:image/jpeg;base64,${base64}\" class=\"thumbnail\">"
                    result = thumbnailPattern.replace(result, thumbnailReplacement)
                }
            }
        }

        // replace new lines
        result = result.replace("\n", "<br>")

        // restore code blocks (after <br> replacement to preserve newlines in <pre>)
        for (i in codeBlocks.indices) {
            result = result.replace("\u0000CODE_BLOCK_$i\u0000", "<pre><code>${codeBlocks[i]}</code></pre>")
        }
        return "<html><head><style>" +
            "blockquote { border-left: 3px solid #ccc; margin: 4px 0; padding: 4px 8px; color: #555; } " +
            "pre { background-color: #f4f4f4; border: 1px solid #ddd; border-radius: 3px; padding: 8px; overflow-x: auto; } " +
            "code { font-family: monospace; } " +
            "table { border-collapse: collapse; margin: 4px 0; } " +
            "th, td { border: 1px solid #ddd; padding: 4px 8px; } " +
            "th { background-color: #f4f4f4; font-weight: bold; } " +
            ".thumbnail { max-width: 200px; max-height: 200px; } " +
            "</style></head><body>$result</body></html>"
    }
}