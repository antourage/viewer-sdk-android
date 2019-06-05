package com.antourage.weaverlib.screens.vod.rv

import android.content.res.Configuration
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.other.models.MessageType

class MessagesAdapter(var list: List<Message>, val orientation: Int) :
    RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        if (orientation == Configuration.ORIENTATION_PORTRAIT)
            return MessageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false))
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
        holder.txtUser.text = list[position].nickname
    }

    fun setMessageList(newlist: List<Message>) {
        val listUserMsg = mutableListOf<Message>()
        for (i in 0 until newlist.size){
            if(newlist[i].type == MessageType.USER)
                listUserMsg.add(newlist[i])
        }
        val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(
            MessageListDiffUtilCallback(list, listUserMsg)
        )
        diffResult.dispatchUpdatesTo(this)
        this.list = listUserMsg
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtUser: TextView = itemView.findViewById(R.id.txtUser)
        val txtMessage: TextView = itemView.findViewById(R.id.txtMessage)
    }
}