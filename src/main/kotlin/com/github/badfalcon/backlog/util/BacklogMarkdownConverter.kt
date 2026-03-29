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

        // show images
        if (attachments != null) {
            for (attachment in attachments) {
                if (!attachment.isImage){
                    continue
                }

                val imagePattern = Regex("""#image\(${Regex.escape(attachment.name)}\)""")
                if (!imagePattern.containsMatchIn(result)) {
                    continue
                }

                val attachmentDatum = attachmentData?.find { it.filename == attachment.name } ?: continue
                val bytes = attachmentDatum.content.readBytes()
                val base64 = Base64.encodeBase64String(bytes)
                val mimeType = guessMimeType(attachment.name)
                val imageReplacement = "<img src=\"data:${mimeType};base64,${base64}\">"
                result = imagePattern.replace(result, imageReplacement)
            }
        }

        // replace remaining new lines (but not after block-level elements)
        result = Regex("""(?<!</h[1-9]>)(?<!</ul>)(?<!</li>)\n""").replace(result, "<br>")
        return "<html><body>$result</body></html>"
    }

    private fun guessMimeType(filename: String): String {
        val ext = filename.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "bmp" -> "image/bmp"
            else -> "image/jpeg"
        }
    }
}