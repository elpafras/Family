package org.sabda.family.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import org.sabda.family.model.MessageData

@Dao
interface MessageDao {
    @Insert
    suspend fun insertMessage(messageData: MessageData)

    @Query("SELECT MAX(counter) FROM messages WHERE chatId = :chatId")
    suspend fun getMaxCounterForChat(chatId: Long): Int

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    suspend fun getMessageByChatId(chatId: Long): List<MessageData>

    @Query("SELECT * FROM messages")
    suspend fun getAllMessages(): List<MessageData>

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesByChatId(chatId: Long)
}