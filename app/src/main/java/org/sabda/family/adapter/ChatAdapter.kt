package org.sabda.family.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.sabda.family.model.MessageData
import org.sabda.family.R

class ChatAdapter(private val messageData: List<MessageData> ) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object{
        const val TYPE_SENT = 1
        const val TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messageData[position].isSent) TYPE_SENT else TYPE_RECEIVED
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return if (viewType == TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_send, parent, false)
            SentViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_receive, parent, false)
            ReceiveViewHolder(view)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val message = messageData[position]
        if (holder is SentViewHolder) {
            holder.textViewMessage.text = message.text
        } else if (holder is ReceiveViewHolder) {
            holder.textViewMessage1.text = message.text
        }
    }

    override fun getItemCount(): Int {
        return messageData.size
    }


    class SentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewMessage: TextView = view.findViewById(R.id.textViewMessage)
    }

    class ReceiveViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewMessage1: TextView = view.findViewById(R.id.textViewMessageReceive)
    }
}