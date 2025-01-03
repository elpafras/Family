package org.sabda.family.utility

class HtmlUtil {
    companion object {
        fun removeHtmlTags(input: String): String {

            var cleanedText = input.replace(Regex("<[^>]*>"), "")

            cleanedText = cleanedText.replace(Regex("&#\\d+;")) { matchResult ->
                val charCode = matchResult.value.removePrefix("&#").removeSuffix(";").toInt()
                charCode.toChar().toString()
            }

            cleanedText = cleanedText.replace(Regex("&[a-zA-Z]+;")) { matchResult ->
                when (matchResult.value) {
                    "&amp;" -> "&"
                    "&lt;" -> "<"
                    "&gt;" -> ">"
                    "&quot;" -> "\""
                    "&apos;" -> "'"
                    else -> matchResult.value
                }
            }

            return cleanedText.trim()
        }

        fun removeRenunganTags(input: String): String{
            return input
                .replace(Regex("<p>"), "")
                .replace(Regex("</p>"), "\n\n")
                .replace(Regex("<br\\s*/?>"), "\n")
                .replace(Regex("<center>"), "")
                .replace(Regex("</center>"), "")

        }

    }
}