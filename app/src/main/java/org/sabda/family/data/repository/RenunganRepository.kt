package org.sabda.family.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.sabda.family.model.NatsData
import org.sabda.family.model.RenunganData
import org.sabda.family.model.VerseData
import org.sabda.family.R
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class RenunganRepository(private val context: Context) {

    private val gson: Gson = GsonBuilder().create()

    /* fetch and parse Renungan */

    fun fetchRenungan(): RenunganData? {
        val urlString = context.getString(R.string.renungan_url)
        val url = URL(urlString)
        return fetchDataFromUrl(url)
    }

    fun fetchRenunganByDate(date: String): RenunganData? {
        val urlString = context.getString(R.string.renungan_by_date_url, date)
        val url = URL(urlString)
        return fetchDataFromUrl(url)
    }

    private fun fetchDataFromUrl(url: URL): RenunganData? {
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        val responseCode = connection.responseCode
        return if (responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.use { it.readText() }
            parseJson(response)
        } else {
            null
        }
    }

    fun fetchVerseTexts(ayat: String): Map<String, String>?{
        val urlString = "https://gpt.sabda.org/api/bible/getData.php?t=/ayat%20$ayat%20tb"
        val url = URL(urlString)

        Log.d("cek url", "fetchVerseTexts: $url")

        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        val responseCode = connection.responseCode
        return if (responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.use { it.readText() }

            Log.d("test response in fetch", "fetchVerseTexts: $response")

            parseFullVerseTexts(response)
        } else {
            null
        }
    }

    fun fetchAllNatsTexts(ayat: String): Map<String, String>? {
        val apiURL = "https://gpt.sabda.org/api/bible/getData.php?t=$ayat%20tb,ayt,kjv,net,jawa"
        val url = URL(apiURL)

        Log.d("cekurl", "fetchAllNatsTexts: $apiURL")

        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        val responseCode = connection.responseCode
        return if (responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.use { it.readText() }
            parseAllAyatTexts(response)
        } else {
            null
        }
    }

    private fun parseJson(jsonResponse: String): RenunganData? {
        return try {
            gson.fromJson(jsonResponse, RenunganData::class.java)
        } catch (e: Exception) {
            Log.e("RenunganRepository", "Error parsing JSON: ${e.message}")
            null
        }
    }

    private fun parseFullVerseTexts(jsonResponse: String): Map<String, String>? {
        val response = gson.fromJson(jsonResponse, VerseData::class.java)

        val textMap = mutableMapOf<String, String>()

        response.text?.results?.su?.data?.results?.forEach { resultDetail ->
            resultDetail.res?.forEach { (_, verseDetail) ->
                val verse = verseDetail.texts?.verse ?: "0"
                val text = verseDetail.texts?.verseText ?: "No Text Available"

                val referenceKey = "($verse)"

                textMap[referenceKey] = text
            }
        }

        Log.d("test response", "parseFullVerseTexts: $response")
        Log.d("test textmap", "parseFullVerseTexts: $textMap")

        return textMap.ifEmpty { null }
    }

    private fun parseAllAyatTexts(jsonResponse: String): Map<String, String>? {
        val response = gson.fromJson(jsonResponse, NatsData::class.java)

        Log.d("cek response", "parseAllAyatTexts: $response")

        val ayatResults = response.natsResults?.natsSu?.natsContent?.natsResults
        val textMap = mutableMapOf<String, String>()

        if (!ayatResults.isNullOrEmpty()) {
            val firstResult = ayatResults[0]
            val ayatKey = firstResult.keys.firstOrNull()
            if (ayatKey != null) {
                val ayatData = firstResult[ayatKey]
                val bibleData = ayatData?.bible

                bibleData?.forEach { (version, versionData) ->
                    val text = versionData.text
                    if (text != null) {
                        textMap[version] = text
                    }
                }
            }
        }
        return textMap.ifEmpty { null }
    }
}