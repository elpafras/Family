package org.sabda.family.model

data class FamilyData(
    val id: String,
    val title: String,
    val date_created: String,
    val date_changed: String,
    val description: String,
    val content: String,
    val summary: String,
    val short: String,
    val text_quote: String,
    val author: String,
    val copyright: String,
    val thumbnail_url: String,
    val source_url: String,
    val media_type: String,
    val categories: List<String>,
    val keywords: List<String>,
    val series: List<String>
)
