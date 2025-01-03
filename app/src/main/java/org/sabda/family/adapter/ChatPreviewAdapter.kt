package org.sabda.family.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.app.AlertDialog
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.sabda.family.R
import org.sabda.family.data.local.MessageDao
import org.sabda.family.model.MessageData

class ChatPreviewAdapter(
    private var chats: List<MessageData>,
    private val lifecyclescope: LifecycleCoroutineScope,
    private val messageDao: MessageDao,
    private val onChatClick: (Long) -> Unit
) : RecyclerView.Adapter<ChatPreviewAdapter.ChatPreviewViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatPreviewViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_preview, parent, false)
        return ChatPreviewViewHolder(view)
    }

    override fun getItemCount(): Int {
        return chats.size
    }

    override fun onBindViewHolder(holder: ChatPreviewViewHolder, position: Int) {
        val preview = chats[position]
        holder.bind(preview)
    }

    fun updateData(newPreviews: List<MessageData>) {
        chats = newPreviews
        notifyDataSetChanged()
    }

    inner class ChatPreviewViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chatPreviewText: TextView = itemView.findViewById(R.id.textViewChatPreview)

        init {
            itemView.setOnClickListener {
                val chatId = chats[adapterPosition].chatId
                onChatClick(chatId)
            }

            itemView.setOnLongClickListener {
                AlertDialog.Builder(itemView.context)
                    .setTitle("Hapus Chat")
                    .setMessage("Apakah Anda yakin ingin menghapus chat ini?")
                    .setPositiveButton("Ya") { _, _ ->
                        deleteChat(adapterPosition) // Call deleteChat function
                    }
                    .setNegativeButton("Tidak", null)
                    .show()
                true
            }
        }

        fun bind(preview: MessageData){
            chatPreviewText.text = preview.text
        }
    }

    private fun deleteChat(position: Int) {
        val chatId = chats[position].chatId
        val mutableChats = chats.toMutableList()

        mutableChats.removeAt(position)
        chats = mutableChats
        notifyItemRemoved(position)

        lifecyclescope.launch(Dispatchers.IO) {
            messageDao.deleteMessagesByChatId(chatId)
        }
    }
}