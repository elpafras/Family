package org.sabda.family.model

import com.google.gson.annotations.SerializedName

data class NatsData(
    @SerializedName("results")
    val natsResults: NatsResultData?
)

data class NatsResultData(
    @SerializedName("su")
    val natsSu: NatsSuData?
)

data class NatsSuData(
    @SerializedName("search")
    val searchText: String?,
    @SerializedName("type")
    val resultCategory: String?,
    @SerializedName("data")
    val natsContent: NatsContentData?
)

data class NatsContentData(
    @SerializedName("results")
    val natsResults: List<Map<String, AyatData>>?
)

data class AyatData(
    @SerializedName("id")
    val id: String?,
    @SerializedName("abbr")
    val abbr: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("chapter")
    val chapter: String?,
    @SerializedName("verse")
    val verse: String?,
    @SerializedName("bible")
    val bible: Map<String, BibleVersionData>?
)

data class BibleVersionData(
    @SerializedName("name")
    val name: String?,
    @SerializedName("year")
    val year: String?,
    @SerializedName("copyright")
    val copyright: Copyright?,
    @SerializedName("text")
    val text: String?
)

data class Copyright(
    @SerializedName("text")
    val text: String?,
    @SerializedName("version")
    val version: String?
)