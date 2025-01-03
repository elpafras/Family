package org.sabda.family.data.repository

import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ChatRepository {
    suspend fun fetchChatResponse(message: String): String? {
        val urlString = "https://dev.sabda.org/unhack/2024/api/chatbot/getChatbot.php?chat="

        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString + message)
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use {
                        val rawResponse = it.readText()
                        return@withContext stripHtml(rawResponse)
                    }
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun stripHtml(html: String): String {
        val cleanText = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
        return cleanText.replace(Regex("\\s+"), " ").trim()
    }

}