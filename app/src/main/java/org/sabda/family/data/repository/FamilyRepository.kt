package org.sabda.family.data.repository

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import org.sabda.family.model.FamilyData
import org.sabda.family.utility.HtmlUtil
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Type
import java.net.HttpURLConnection
import java.net.URL

class FamilyRepository {

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(object : TypeToken<List<String>>() {}.type, SeriesDeserializer())
        .create()

    fun fetchFamilyData(): List<FamilyData> {
        return fetchFilteredFamilyData().map { familyData ->
            familyData.copy(
                description = HtmlUtil.removeHtmlTags(familyData.description),
                content = HtmlUtil.removeHtmlTags(familyData.content),
                summary = HtmlUtil.removeHtmlTags(familyData.summary)
            )
        }
    }

    fun fetchFilteredFamilyData(itemTitle: String? = null, query: String? = null): List<FamilyData> {
        val url = if (itemTitle != null && query != null){
            URL("https://dev.sabda.org/unhack/2024/api/family/getData.php?q=$itemTitle&term=$query")
        } else {
            URL("https://dev.sabda.org/unhack/2024/api/family/getData.php?size=1000")
        }
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        val responseCode = connection.responseCode
        return if (responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.use { it.readText() }

            // Parse JSON response to List<FamilyData>
            parseJson(response)
        } else {
            emptyList()
        }
    }

    private fun parseJson(jsonResponse: String): List<FamilyData> {
        val rootObject      = JSONObject(jsonResponse)
        val jsonArray       = rootObject.getJSONArray("result")
        val jsonArrayString = jsonArray.toString()

        val familyDataType = object : TypeToken<List<FamilyData>>() {}.type
        return gson.fromJson(jsonArrayString, familyDataType)
    }

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