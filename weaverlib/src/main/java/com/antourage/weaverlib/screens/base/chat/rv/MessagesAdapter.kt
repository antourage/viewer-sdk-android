package com.antourage.weaverlib.screens.base.chat.rv

import android.content.res.Configuration
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.other.models.MessageType
import com.squareup.picasso.Picasso

class MessagesAdapter(var list: List<Message>, val orientation: Int) :
    RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MessageViewHolder(
        LayoutInflater.from(parent.context).inflate(
            if (orientation == Configuration.ORIENTATION_PORTRAIT)
                R.layout.item_message_portrait else R.layout.item_message_landscape,
            parent, false
        )
    )

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val messageItem = list[position]
        with(messageItem) {
            holder.txtMessage.text = text
            if (!avatarUrl.isNullOrEmpty()) {
                Picasso.get().load(avatarUrl)
                    .placeholder(R.drawable.ic_default_user)
                    .error(R.drawable.ic_default_user)
                    .into(holder.ivAvatar)
            }
            holder.txtUser.text = nickname
        }
    }

    fun setMessageList(newList: List<Message>) {
        val userMessagesList = newList.filter { it.type == MessageType.USER }
        val diffResult = DiffUtil.calculateDiff(MessageListDiffUtilCallback(list, userMessagesList))

        this.list = userMessagesList
        diffResult.dispatchUpdatesTo(this)
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        val txtUser: TextView = itemView.findViewById(R.id.txtUser)
        val txtMessage: TextView = itemView.findViewById(R.id.txtMessage)
    }
}