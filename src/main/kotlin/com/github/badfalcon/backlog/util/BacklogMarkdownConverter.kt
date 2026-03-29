package com.github.badfalcon.backlog.util

import com.nulabinc.backlog4j.Attachment
import com.nulabinc.backlog4j.AttachmentData
import org.apache.commons.codec.binary.Base64

class BacklogMarkdownConverter {

    fun toHtml(src: String, attachments: MutableList<Attachment>?, attachmentData: MutableList<AttachmentData>?): String {
        var result = src

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

        // replace code blocks
        val codePattern = Regex("""\{code\}(.*?)\{/code\}""", RegexOption.DOT_MATCHES_ALL)
        result = codePattern.replace(result, "<pre><code>$1</code></pre>")

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

        // show images
        if (attachments != null) {
            for (attachment in attachments) {
                if (!attachment.isImage){
                    continue
                }

                val imagePattern = Regex("""#image\(${attachment.name}\)""")
                if (!imagePattern.containsMatchIn(result)) {
                    continue
                }

                val attachmentDatum = attachmentData?.find { it.filename == attachment.name } ?: continue
                val bytes = attachmentDatum.content.readBytes()
                val base64 = Base64.encodeBase64String(bytes)
                val imageReplacement = "<img src=\"data:image/jpeg;base64,${base64}\">"
                result = imagePattern.replace(result, imageReplacement)
            }
        }

        // replace new lines
        result = result.replace("\n", "<br>")
        return "<html><head><style>" +
            "blockquote { border-left: 3px solid #ccc; margin: 4px 0; padding: 4px 8px; color: #555; } " +
            "pre { background-color: #f4f4f4; border: 1px solid #ddd; border-radius: 3px; padding: 8px; overflow-x: auto; } " +
            "code { font-family: monospace; }" +
            "</style></head><body>$result</body></html>"
    }
}