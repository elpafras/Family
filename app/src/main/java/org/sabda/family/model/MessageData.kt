package org.sabda.family.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageData(
    var text: String,
    val isSent: Boolean,
    val chatId: Long,
    val timestamp: Long,
    val counter: Int,
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)
