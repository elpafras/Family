package org.sabda.family

import android.os.Bundle
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.sabda.family.adapter.ChatAdapter
import org.sabda.family.adapter.ChatPreviewAdapter
import org.sabda.family.base.BaseActivity
import org.sabda.family.data.local.AppDatabase
import org.sabda.family.data.local.MessageDao
import org.sabda.family.data.repository.ChatRepository
import org.sabda.family.databinding.ActivityMainBinding
import org.sabda.family.fragment.ChatFragment
import org.sabda.family.model.MessageData
import org.sabda.family.utility.LoadingUtil
import org.sabda.family.utility.MenuUtil
import org.sabda.family.utility.NetworkUtil
import org.sabda.family.utility.StatusBarUtil

class MainActivity : BaseActivity<ActivityMainBinding>(), ChatFragment.ChatFragmentCallback {

    override fun setupViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var chatRepository: ChatRepository
    private lateinit var loadingUtil: LoadingUtil
    private lateinit var appDatabase: AppDatabase
    private lateinit var messageDao: MessageDao

    private val messageList: MutableList<MessageData> = mutableListOf()
    private val chatPreview: MutableList<MessageData> = mutableListOf()
    private var currentChatId = System.currentTimeMillis()
    private var chatCounter = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!NetworkUtil.isInternetAvailable(this)) {
            NetworkUtil.showNoInternetDialog(this)
            return
        }

        initDatabase()
        StatusBarUtil().setLightStatusBar(this, R.color.white)

        chatRepository = ChatRepository()
        loadingUtil = LoadingUtil()

        setupButtons()
        setupRecyclerView()
        setupNavigationView()

        currentChatId = getSharedPreferences("ChatPrefs", MODE_PRIVATE).getLong("currentChatId", currentChatId)
        intent.getLongExtra("CHAT_ID", -1L).takeIf { it != -1L }?.let { chatId ->
            currentChatId = chatId
            saveCurrentChatId(chatId)
        }

        if (intent.getBooleanExtra("START_NEW_CHAT", false)) {
            startNewChat()
        }

        loadMessagesFromDatabase()
        loadChatPreviews()
    }

    private fun initDatabase() {
        appDatabase = AppDatabase.getDatabase(this)
        messageDao = appDatabase.messageDao()
    }


    private fun setupButtons() {
        binding.btnSendMessage.setOnClickListener {
            Log.d("TAG", "setupButtons: clicked")
            val messageText = binding.editTextMessage.text.toString()
            if (messageText.isNotEmpty()) {
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
            with(binding.drawerLayout) {
                if (isDrawerOpen(binding.navView)) closeDrawer(binding.navView) else openDrawer(binding.navView)
            }
        }

        binding.newChatImageView.setOnClickListener {
            startNewChat()
        }

        binding.option.setOnClickListener { MenuUtil(this).setupMenu(it, this::class.java.simpleName) }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messageList)
        Log.d("MainActivity", "RecyclerView updated with ${messageList.size} messages")
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = chatAdapter
        }
    }

    private fun setupNavigationView() {
        val chatPreviewAdapter = ChatPreviewAdapter(chatPreview, lifecycleScope, messageDao) { chatId ->
            Log.d("ChatPreview", "Chat clicked: $chatId")
            currentChatId = chatId
            saveCurrentChatId(chatId)
            Log.d("MainActivity", "Updated currentChatId: $currentChatId")
            loadChatMessagesByChatId(chatId)
            binding.drawerLayout.closeDrawer(binding.navView)
        }

        val recyclerViewChatPreviews: RecyclerView = binding.navView.findViewById(R.id.recyclerViewPreview)
        recyclerViewChatPreviews.layoutManager = LinearLayoutManager(this)
        recyclerViewChatPreviews.adapter = chatPreviewAdapter

        loadChatPreviews()
    }

    private fun loadChatPreviews() {
        messageDao.getAllMessages().observe(this) { previews ->
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

            (binding.navView.findViewById<RecyclerView>(R.id.recyclerViewPreview).adapter as ChatPreviewAdapter).updateData(previewList)
        }
    }

    private fun startNewChat() {
        currentChatId = System.currentTimeMillis()
        chatCounter = 1
        messageList.clear()
        saveCurrentChatId(currentChatId)
        binding.recyclerView.scrollToPosition(0)
    }

    private fun sendMessage(messageText: String) {
        if (!NetworkUtil.isInternetAvailable(this)) {
            NetworkUtil.showNoInternetDialog(this)
            return
        }

        val newMessage = MessageData(messageText, true, currentChatId, timestamp = System.currentTimeMillis(), chatCounter)
        messageList.add(newMessage)
        chatAdapter.notifyItemInserted(messageList.lastIndex)
        binding.recyclerView.scrollToPosition(messageList.lastIndex)
        lifecycleScope.launch(Dispatchers.IO) { messageDao.insertMessage(newMessage) }
        chatCounter++
    }

    private fun receiveMessage(response: String) {
        val jsonResponse = JSONObject(response)
        val botResponse = jsonResponse.getString("response")

        val receivedMessage = MessageData(botResponse, false, currentChatId, timestamp = System.currentTimeMillis(), chatCounter)
        messageList.add(receivedMessage)
        chatAdapter.notifyItemInserted(messageList.size - 1)
        binding.recyclerView.scrollToPosition(messageList.size - 1)

        lifecycleScope.launch(Dispatchers.IO) {
            messageDao.insertMessage(receivedMessage)
            Log.d(
                "MainActivity",
                "Received message for chatId: $currentChatId with counter: $chatCounter"
            )
        }
    }

    private fun loadMessagesFromDatabase() {
        lifecycleScope.launch {
            val messages = withContext(Dispatchers.IO) { messageDao.getMessageByChatId(currentChatId) }
            messageList.addAll(messages)
            binding.recyclerView.scrollToPosition(messageList.size - 1)
        }
    }

    private fun loadChatMessagesByChatId(chatId: Long) {
        lifecycleScope.launch {
            messageList.clear()
            chatAdapter.notifyDataSetChanged()
            val messages = withContext(Dispatchers.IO) { messageDao.getMessageByChatId(chatId) }
            Log.d("MainActivity", "Loaded messages: ${messages.size}")
            messageList.addAll(messages)
            chatCounter = (messages.lastOrNull()?.counter ?: 0) + 1
            chatAdapter.notifyDataSetChanged()
            binding.recyclerView.scrollToPosition(messageList.size - 1)
        }
    }

    private fun saveCurrentChatId(chatId: Long) {
        getSharedPreferences("ChatPrefs", MODE_PRIVATE).edit { putLong("currentChatId", chatId) }
    }

    override fun onLoadChatMessagesByChatId(chatId: Long) {
        loadChatMessagesByChatId(chatId)
    }
}
