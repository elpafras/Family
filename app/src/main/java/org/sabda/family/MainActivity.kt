package org.sabda.family

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.sabda.family.adapter.ChatAdapter
import org.sabda.family.adapter.ChatPreviewAdapter
import org.sabda.family.data.local.AppDatabase
import org.sabda.family.data.local.MessageDao
import org.sabda.family.data.repository.ChatRepository
import org.sabda.family.databinding.ActivityMainBinding
import org.sabda.family.fragment.ChatFragment
import org.sabda.family.model.MessageData
import org.sabda.family.utility.LoadingUtil
import org.sabda.family.utility.MenuUtil
import org.sabda.family.utility.StatusBarUtil

class MainActivity : AppCompatActivity(), ChatFragment.ChatFragmentCallback {

    private lateinit var binding: ActivityMainBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var chatRepository: ChatRepository
    private lateinit var loadingUtil: LoadingUtil
    private lateinit var appDatabase: AppDatabase
    private lateinit var messageDao: MessageDao
    private val messageList: MutableList<MessageData> = mutableListOf()
    private val chatPreview: MutableList<MessageData> = mutableListOf()

    private var currentChatId = System.currentTimeMillis()
    private var chatCounter  = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appDatabase = AppDatabase.getDatabase(this)
        messageDao = appDatabase.messageDao()

        val chatId = intent.getLongExtra("CHAT_ID", -1)
        if (chatId != -1L) {
            loadChatMessagesByChatId(chatId)
        }

        StatusBarUtil().setLightStatusBar(this, R.color.white)
        chatRepository = ChatRepository()

        setupButtons()
        setupRecyclerView()
        setupNavigationView()

        loadingUtil = LoadingUtil()

        //startNewChat()
        loadMessagesFromDatabase()
        loadChatPreviews()
    }

    private fun setupButtons() {
        binding.btnSendMessage.setOnClickListener{
            Log.d("TAG", "setupButtons: clicked")
            val messageText = binding.editTextMessage.text.toString()
            if (messageText.isNotEmpty()){
                sendMessage(messageText)
                binding.editTextMessage.text.clear()
                loadingUtil.showLoadingMessage(chatAdapter, messageList, currentChatId)

                lifecycleScope.launch {
                    val response = chatRepository.fetchChatResponse(messageText)
                    response?.let {
                        loadingUtil.hideLoadingMessage(chatAdapter, messageList)
                        receiveMessage(it)
                    }
                }
            }
        }

        binding.btnHistory.setOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(binding.navView)){
                binding.drawerLayout.closeDrawer(binding.navView)
            } else {
                binding.drawerLayout.openDrawer(binding.navView)
            }
        }

        binding.newChatImageView.setOnClickListener {
            startNewChat()
        }

        binding.option.setOnClickListener { MenuUtil(this).setupMenu(it, this::class.java.simpleName) }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messageList)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = chatAdapter
        }
    }

    private fun setupNavigationView() {
        val chatPreviewAdapter = ChatPreviewAdapter(chatPreview, lifecycleScope, messageDao) { chatId ->
            loadChatMessagesByChatId(chatId)
            binding.drawerLayout.closeDrawer(binding.navView)
        }

        val recyclerViewChatPreviews: RecyclerView = binding.navView.findViewById(R.id.recyclerViewPreview)
        recyclerViewChatPreviews.layoutManager = LinearLayoutManager(this)
        recyclerViewChatPreviews.adapter = chatPreviewAdapter

        loadChatPreviews()
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
                Log.d("MainActivity", "Loaded preview for chatId: ${it.key} with counter: ${previewMessage.counter}, text: ${previewMessage.text}")

                if (existingPreviews.contains(Pair(chatId, counter))) {
                    Log.d("MainActivity", "Skipping duplicate preview for chatId: $chatId with counter: $counter")
                    return@mapNotNull null
                } else {
                    existingPreviews.add(Pair(chatId, counter))

                    Log.d("MainActivity", "Adding preview for chatId: $chatId with counter: $counter, text: ${previewMessage.text}")

                }
                MessageData(
                    text = previewMessage.text,
                    isSent = previewMessage.isSent,
                    chatId = chatId,
                    timestamp = previewMessage.timestamp,
                    counter = counter
                )
            }
            (binding.navView.findViewById<RecyclerView>(R.id.recyclerViewPreview).adapter as ChatPreviewAdapter).updateData(previewList)
        }
    }

    private fun startNewChat() {
        lifecycleScope.launch {
            chatCounter = withContext(Dispatchers.IO) {
                messageDao.getMaxCounterForChat(currentChatId) + 1
            }
            Log.d("counter in startnewchat", "startNewChat: $chatCounter & $currentChatId")
        }
        messageList.clear()
        chatAdapter.notifyDataSetChanged()
        currentChatId = System.currentTimeMillis()
    }

    private fun sendMessage(messageText: String) {
        val newMessage = MessageData(messageText, true, currentChatId, timestamp = System.currentTimeMillis(), chatCounter)
        messageList.add(newMessage)
        chatAdapter.notifyItemInserted(messageList.size - 1)
        binding.recyclerView.scrollToPosition(messageList.size - 1)

        lifecycleScope.launch(Dispatchers.IO) {
            messageDao.insertMessage(newMessage)
            Log.d("counter in sendmessage", "sendMessage: $chatCounter & $currentChatId")
        }

        loadChatPreviews()
    }

    private fun receiveMessage(response: String) {
        val jsonResponse = JSONObject(response)
        val botResponse = jsonResponse.getString("response")

        val receivedMessage = MessageData(botResponse, false, currentChatId, timestamp = System.currentTimeMillis(), chatCounter)
        messageList.add(receivedMessage)
        chatAdapter.notifyItemInserted(messageList.size -1)
        binding.recyclerView.scrollToPosition(messageList.size -1)

        lifecycleScope.launch(Dispatchers.IO) {
            messageDao.insertMessage(receivedMessage)
            Log.d("counter in receiveMessage", "receiveMessage: $chatCounter & $currentChatId")
        }

        loadChatPreviews()
    }

    private fun loadMessagesFromDatabase() {
        lifecycleScope.launch {
            val messages = withContext(Dispatchers.IO) { messageDao.getMessageByChatId(currentChatId) }
            messageList.addAll(messages)
            chatAdapter.notifyDataSetChanged()
            binding.recyclerView.scrollToPosition(messageList.size -1)
        }
    }

    private fun loadChatMessagesByChatId(chatId: Long) {
        messageList.clear()

        lifecycleScope.launch {
            val messages = withContext(Dispatchers.IO) {
                messageDao.getMessageByChatId(chatId)
            }
            messageList.addAll(messages)

            if (messages.isNotEmpty()) {
                chatCounter = messages.first().counter
            }

            Log.d("counter in loadChatMessagesByChatId", "loadChatMessagesByChatId: $chatCounter & $chatId")

            chatAdapter.notifyDataSetChanged()
            binding.recyclerView.scrollToPosition(messageList.size -1)
        }
    }

    override fun onLoadChatMessagesByChatId(chatId: Long) {
        loadChatMessagesByChatId(chatId)
    }
}