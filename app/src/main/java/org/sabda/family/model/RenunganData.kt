package org.sabda.family.model

data class RenunganData(
    val id: String,
    val date: String,
    val verse_1: String,
    val verse_2: String?,
    val nats: String,
    val nats_verse: String,
    val title: String,
    val content: String,
    val footer: String
)
