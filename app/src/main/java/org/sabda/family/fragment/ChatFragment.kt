package org.sabda.family.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sabda.family.MainActivity
import org.sabda.family.adapter.ChatPreviewAdapter
import org.sabda.family.data.local.AppDatabase
import org.sabda.family.data.local.MessageDao
import org.sabda.family.databinding.FragmentChatBinding
import org.sabda.family.model.MessageData

class ChatFragment : Fragment() {

    interface ChatFragmentCallback {
        fun onLoadChatMessagesByChatId(chatId: Long)
    }

    private var callback: ChatFragmentCallback? = null

    private lateinit var binding: FragmentChatBinding
    private lateinit var messageDao: MessageDao
    private val chatPreview: MutableList<MessageData> = mutableListOf()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ChatFragmentCallback) {
            callback = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChatBinding.inflate(inflater, container, false)

        messageDao = AppDatabase.getDatabase(requireContext()).messageDao()

        setupRecyclerView()
        setupButtons()
        loadChatPreviews()

        return binding.root
    }

    private fun setupButtons() {
        binding.floatingChatButton.setOnClickListener { startActivity(Intent(context, MainActivity::class.java)) }
    }

    private fun setupRecyclerView() {
        binding.previewRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ChatPreviewAdapter(chatPreview, lifecycleScope, messageDao) { chatId ->
                loadChatMessagesByChatId(chatId)
            }
        }
    }

    private fun loadChatPreviews() {
        lifecycleScope.launch {
            val previews = withContext(Dispatchers.IO) {
                messageDao.getAllMessages()
            }

            val existingPreviews = mutableSetOf<Pair<Long, Int>>()

            val previewList = previews.groupBy { it.chatId }.mapNotNull {
                val previewMessage = it.value.first()
                val chatId = it.key
                val counter = previewMessage.counter

                Log.d("ChatFragment", "Loaded preview for chatId: $chatId with counter: $counter, text: ${previewMessage.text}")

                if (existingPreviews.contains(Pair(chatId, counter))) {
                    Log.d("ChatFragment", "Skipping duplicate preview for chatId: $chatId with counter: $counter")
                    return@mapNotNull null
                } else {
                    existingPreviews.add(Pair(chatId, counter))
                    Log.d("ChatFragment", "Adding preview for chatId: $chatId with counter: $counter, text: ${previewMessage.text}")
                    MessageData(
                        text = previewMessage.text,
                        isSent = previewMessage.isSent,
                        chatId = chatId,
                        timestamp = previewMessage.timestamp,
                        counter = counter
                    )
                }
            }
            (binding.previewRecyclerView.adapter as? ChatPreviewAdapter)?.updateData(previewList)
        }
    }

    private fun loadChatMessagesByChatId(chatId: Long) {
        callback?.onLoadChatMessagesByChatId(chatId)

        val intent = Intent(activity, MainActivity::class.java).apply {
            putExtra("CHAT_ID", chatId)
        }

        startActivity(intent)
    }


}