package org.sabda.family.utility

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import org.sabda.family.adapter.ChatAdapter
import org.sabda.family.model.MessageData

class LoadingUtil {

    private val handler = Handler(Looper.getMainLooper())
    private var loadingIndex: Int = -1

    fun showLoadingMessage(chatAdapter: ChatAdapter, messageList: MutableList<MessageData>, chatId: Long) {
        val loading = arrayOf("Loading.", "Loading..", "Loading...")
        handler.post(object : Runnable {
            var index = 0
            override fun run() {
                if (loadingIndex == -1) {
                    messageList.add(MessageData(loading[index], false, chatId, System.currentTimeMillis(), 0))
                    loadingIndex = messageList.size - 1
                    chatAdapter.notifyItemInserted(loadingIndex)
                } else {
                    messageList[loadingIndex].text = loading[index]
                    chatAdapter.notifyItemChanged(loadingIndex)
                }
                index = (index + 1) % loading.size
                handler.postDelayed(this, 500)
            }
        })
    }

    fun hideLoadingMessage(chatAdapter: ChatAdapter, messageList: MutableList<MessageData>) {
        if (loadingIndex != -1) {
            messageList.removeAt(loadingIndex)
            chatAdapter.notifyItemRemoved(loadingIndex)
            loadingIndex = -1
        }
        handler.removeCallbacksAndMessages(null)
    }

    fun showLoadingWebView(loadingTextView: View) {
        val loadingMessages = arrayOf("Loading.", "Loading..", "Loading...")
        loadingTextView.visibility = View.VISIBLE
        handler.post(object : Runnable {
            var index = 0
            override fun run() {
                if (loadingTextView is TextView) {
                    loadingTextView.text = loadingMessages[index]
                }
                index = (index + 1) % loadingMessages.size
                handler.postDelayed(this, 500)
            }
        })
    }

    fun hideLoadingWebView(loadingTextView: View) {
        loadingTextView.visibility = View.GONE
        handler.removeCallbacksAndMessages(null)
    }
}