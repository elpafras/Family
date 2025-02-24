package org.sabda.family.utility

import kotlin.text.Regex

class HtmlUtil {
    companion object {
        // Mapping entity HTML yang ingin didekode
        private val htmlEntityMap = mapOf(
            "&amp;"  to "&",
            "&lt;"   to "<",
            "&gt;"   to ">",
            "&quot;" to "\"",
            "&apos;" to "'"
        )

        /**
         * Menghapus tag HTML secara umum serta mendekode entitas HTML.
         */
        fun removeHtmlTags(input: String): String =
            input
                // Hapus semua tag HTML
                .replace(Regex("<[^>]*>"), "")
                // Dekode entitas numerik, misalnya "&#65;" -> "A"
                .replace(Regex("&#\\d+;")) { matchResult ->
                    matchResult.value
                        .removeSurrounding("&#", ";")
                        .toIntOrNull()
                        ?.toChar()
                        ?.toString() ?: matchResult.value
                }
                // Dekode entitas HTML dengan nama, misalnya "&amp;" -> "&"
                .replace(Regex("&[a-zA-Z]+;")) { matchResult ->
                    htmlEntityMap[matchResult.value] ?: matchResult.value
                }
                .trim()

        /**
         * Menghapus atau mengganti tag HTML tertentu untuk mendapatkan teks yang lebih bersih.
         */
        fun removeSpesificTags(input: String): String {
            // Daftar pasangan (Regex, pengganti)
            val replacements = listOf(
                Regex("<p>") to "\n\n",
                Regex("</p>") to "\n\n",
                Regex("<br\\s*/?>") to "\n",
                Regex("<center>") to "",
                Regex("</center>") to "",
                Regex("\\s{2,}") to " ",
                Regex("<article>") to "",
                Regex("</article>") to "",
                Regex("<h1>") to "",
                Regex("</h1>") to "\n\n",
                Regex("<ul>") to "",
                Regex("</ul>") to "",
                Regex("<li>") to "- ",
                Regex("</li>") to "\n\n",
                Regex("<ol>") to "\n\n",
                Regex("</ol>") to "",
                Regex("<blockquote>") to "\n\n>>> ",
                Regex("</blockquote>") to "\n",
                Regex("<strong>|<b>") to "",
                Regex("</strong>|</b>") to "",
                Regex("<em>|<i>") to "",
                Regex("</em>|</i>") to "",
                Regex("<u>|</u>") to "",
                Regex("<h3>") to "\n",
                Regex("</h3>") to "\n",
                Regex("<html>|</html>") to "",
                Regex("<head>|</head>") to "",
                Regex("<body>|</body>") to "",
                Regex("<title>") to "",
                Regex("</title>") to "\n\n",
                Regex("<h2>") to "",
                Regex("</h2>") to "\n\n",
                Regex("<section>") to "",
                Regex("</section>") to "\n\n",
                Regex("<a href=\".*?\">") to "",
                Regex("</a>") to "",
                Regex("<!DOCTYPE html>") to "",
                Regex("<html lang=\"id\">") to "",
                Regex("<meta charset=\"UTF-8\">") to "",
                Regex("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">") to ""
            )

            // Terapkan semua penggantian secara berurutan
            return replacements.fold(input) { currentText, (pattern, replacement) ->
                pattern.replace(currentText, replacement)
            }.trim()
        }
    }
}