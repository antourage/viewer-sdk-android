package com.antourage.weaverlib.screens.base.chat.rv

import android.content.res.Configuration
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.millisToTime
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.other.models.MessageType

internal class MessagesAdapter(var list: MutableList<Message>, val orientation: Int,
                               private val startTimeLong : Long?) :
    RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MessageViewHolder(
        LayoutInflater.from(parent.context).inflate(
            if (orientation == Configuration.ORIENTATION_PORTRAIT)
                R.layout.item_message_portrait
            else
                R.layout.item_message_landscape,
            parent, false
        )
    )

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val messageItem = list[position]
        with(messageItem) {
            with(holder) {
                txtMessage.text = text
                txtUser.text = nickname
                txtTimeAdded.text = when {
                    pushTimeMills != null -> pushTimeMills?.millisToTime()
                    startTimeLong != null -> {
                        ( ((timestamp?.seconds ?: 0) * 1000) - startTimeLong).millisToTime()
                    }
                    else -> txtTimeAdded.context.getString(R.string.ant_init_time)
                }
            }
        }
    }

    fun setMessageList(newList: List<Message>) {
        val userMessagesList = newList.filter { it.type == MessageType.USER }
        val diffResult = DiffUtil.calculateDiff(MessageListDiffUtilCallback(list, userMessagesList))

        this.list.clear()
        this.list.addAll(userMessagesList)
        diffResult.dispatchUpdatesTo(this)
    }

    fun newMessagesWereAdded(list: List<Message>?): Boolean {
        return (list?.size ?: 0 > this.list.size)
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtUser: TextView = itemView.findViewById(R.id.txtUser)
        val txtMessage: TextView = itemView.findViewById(R.id.txtMessage)
        val txtTimeAdded: TextView = itemView.findViewById(R.id.txtTimeAt)
    }
}