package org.sabda.family.model

import com.google.gson.annotations.SerializedName

data class VerseData(
    @SerializedName("text") val text: TextData?
)

data class TextData(
    @SerializedName("results") val results: ResultsData?
)

data class ResultsData(
    @SerializedName("su") val su: SuData?
)

data class SuData(
    @SerializedName("data") val data: DataResults?
)

data class DataResults(
    @SerializedName("results") val results: List<ResultDetail>?
)

data class ResultDetail(
    @SerializedName("ref") val reference: String?,
    @SerializedName("res") val res: Map<String, VerseDetail>?
)

data class VerseDetail(
    @SerializedName("texts") val texts: TextContent?
)

data class TextContent(
    @SerializedName("abbr")
    val abbr: String?,
    @SerializedName("chapter")
    val chapter: String?,
    @SerializedName("verse")
    val verse: String?,
    @SerializedName("text") val verseText: String?
)