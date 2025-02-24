package org.sabda.family.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import org.sabda.family.MainActivity
import org.sabda.family.adapter.ChatPreviewAdapter
import org.sabda.family.base.BaseFragment
import org.sabda.family.data.local.AppDatabase
import org.sabda.family.data.local.MessageDao
import org.sabda.family.databinding.FragmentChatBinding
import org.sabda.family.model.MessageData
import org.sabda.family.utility.NetworkUtil

class ChatFragment : BaseFragment<FragmentChatBinding>() {

    interface ChatFragmentCallback {
        fun onLoadChatMessagesByChatId(chatId: Long)
    }

    private var callback: ChatFragmentCallback? = null

    private lateinit var messageDao: MessageDao
    private val chatPreview: MutableList<MessageData> = mutableListOf()

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentChatBinding.inflate(inflater, container, false)

    override fun onBackPressed() {
        // Misalnya tampilkan konfirmasi sebelum keluar dari aplikasi
        Toast.makeText(requireContext(), "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()
    }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!NetworkUtil.isInternetAvailable(requireContext())) {
            NetworkUtil.showNoInternetDialog(requireContext())
            return
        }

        messageDao = AppDatabase.getDatabase(requireContext()).messageDao()
        setupRecyclerView()
        loadChatPreviews()

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
        messageDao.getAllMessages().observe(viewLifecycleOwner) { previews ->
            val existingPreviews = mutableSetOf<Pair<Long, Int>>()

            val previewList = previews.groupBy { it.chatId }.mapNotNull {
                val previewMessage = it.value.first()
                val chatId = it.key
                val counter = previewMessage.counter

                if (existingPreviews.contains(Pair(chatId, counter))) {
                    return@mapNotNull null
                } else {
                    existingPreviews.add(Pair(chatId, counter))
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