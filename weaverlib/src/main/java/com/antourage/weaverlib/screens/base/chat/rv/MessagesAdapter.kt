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
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        if (orientation == Configuration.ORIENTATION_PORTRAIT)
            return MessageViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_message,
                    parent,
                    false
                )
            )
        else
            return MessageViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_message_fullscreen,
                    parent,
                    false
                )
            )
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.txtMessage.text = list[position].text
        if (!list[position].avatarUrl.isNullOrEmpty())
            Picasso.get().load(list[position].avatarUrl)
                .placeholder(R.drawable.ic_default_user)
                .error(R.drawable.ic_default_user)
                .into(holder.ivAvatar)
        holder.txtUser.text = list[position].nickname
    }

    fun setMessageList(newList: List<Message>) {
        val listUserMsg = mutableListOf<Message>()
        for (i in 0 until newList.size) {
            if (newList[i].type == MessageType.USER)
                listUserMsg.add(newList[i])
        }
        val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(
            MessageListDiffUtilCallback(list, listUserMsg)
        )
        diffResult.dispatchUpdatesTo(this)
        this.list = listUserMsg
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        val txtUser: TextView = itemView.findViewById(R.id.txtUser)
        val txtMessage: TextView = itemView.findViewById(R.id.txtMessage)
    }
}