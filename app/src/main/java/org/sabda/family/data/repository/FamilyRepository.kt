package org.sabda.family.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import org.sabda.family.R
import org.sabda.family.model.FamilyData
import org.sabda.family.utility.NetworkUtil.isInternetAvailable
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Type
import java.net.HttpURLConnection
import java.net.URL

class FamilyRepository(private val context: Context) {

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(object : TypeToken<List<String>>() {}.type, SeriesDeserializer())
        .create()

    /**
     * Fetches family data based on parameters.
     */
    fun fetchFamilyData(
        id: String? = null,
        fragmentName: String? = null,
        itemTitle: String? = null,
        query: String? = null
    ): List<FamilyData> {
        if (!isInternetAvailable(context)) {
            Log.e("FamilyRepository", "Tidak ada koneksi internet.")
            return emptyList()
        }

        val size = determineSize(fragmentName)
        val url = buildUrl(id, itemTitle, query, size)
        val connection = openConnection(url)

        if (connection == null) {
            Log.e("FamilyRepository", "Gagal membuka koneksi. Pastikan internet tersedia!")
            return emptyList()
        }

        return try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                if (id != null) {
                    listOfNotNull(parseJsonById(response)) // Wrap single item into a list
                } else {
                    parseJson(response)
                }
            } else {
                logError("HTTP error: ${connection.responseCode}")
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Parses the JSON response for a list of FamilyData objects.
     */
    private fun parseJson(jsonResponse: String): List<FamilyData> {
        val jsonArray = JSONObject(jsonResponse).getJSONArray("result")
        return gson.fromJson(jsonArray.toString(), object : TypeToken<List<FamilyData>>() {}.type)
    }

    /**
     * Parses the JSON response for a single FamilyData object.
     */
    private fun parseJsonById(jsonResponse: String): FamilyData? {
        val jsonObject = JSONObject(jsonResponse)
        val jsonArray = jsonObject.optJSONArray("result") // Ambil array

        Log.d("FamilyRepository", "Raw JSON response: $jsonResponse")

        return if (jsonArray != null && jsonArray.length() > 0) {
            val resultObject = jsonArray.getJSONObject(0) // Ambil elemen pertama dari array
            Log.d("FamilyRepository", "Parsed JSON Object: $resultObject")
            gson.fromJson(resultObject.toString(), FamilyData::class.java)
        } else {
            Log.e("FamilyRepository", "No valid 'result' data found")
            null
        }
    }

    /**
     * Builds the URL based on itemTitle and query parameters.
     */
    private fun buildUrl(id: String? = null, itemTitle: String? = null, query: String? = null, size: Int? = null): URL {
        val baseUrl = context.getString(R.string.base_family_data_url)
        Log.d("cek id, itemtitle dan query + size", "buildUrl: id = $id, itemtitle = $itemTitle, query = $query, size = $size")
        val urlString = when {
            id != null -> "$baseUrl?id=$id"
            itemTitle != null && query != null -> "$baseUrl?q=$itemTitle&term=$query"
            else -> "$baseUrl?size=$size"
        }
        Log.d("cek url", "buildUrl: $urlString")
        return URL(urlString)
    }

    /**
     * Opens an HTTP connection for the given URL.
     */
    private fun openConnection(url: URL): HttpURLConnection? {
        return try {
            (url.openConnection() as? HttpURLConnection)?.apply {
                requestMethod = "GET"
                connectTimeout = 5000  // Tambahkan timeout (5 detik)
                readTimeout = 5000
            }
        } catch (e: java.net.UnknownHostException) {
            Log.e("FamilyRepository", "Tidak bisa terhubung ke server. Periksa koneksi internet!", e)
            null
        } catch (e: Exception) {
            Log.e("FamilyRepository", "Gagal membuka koneksi: ${e.localizedMessage}", e)
            null
        }
    }

    /**
     * Determines the size parameter based on the fragment name.
     */
    private fun determineSize(fragmentName: String?): Int {
        return when (fragmentName) {
            "HomeFragment" -> 4
            "ResourcesFragment" -> 500
            else -> -1
        }
    }

    /**
     * Logs error messages.
     */
    private fun logError(message: String) {
        Log.e("FamilyRepository", message)
    }

    /**
     * Custom deserializer to handle cases where a JSON field can be a string or an array.
     */
    private class SeriesDeserializer : JsonDeserializer<List<String>> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): List<String> {
            return if (json.isJsonArray){
                json.asJsonArray.map { it.asString }
            } else {
                listOf(json.asString)
            }
        }
    }
}