package com.antourage.weaverlib.screens.weaver.rv

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.models.Message

class MessagesAdapter (var list:List<Message>, val orientation:Int):RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>(){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        if(orientation == Configuration.ORIENTATION_PORTRAIT)
            return MessageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false))
        else
            return MessageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_message_fullscreen, parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.txtMessage.text = list[position].text
        holder.txtUser.text = list[position].nickname
    }

    fun setMessageList(newlist: List<Message>) {
        val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(
            MessageListDiffUtilCallback(list,newlist))
        diffResult.dispatchUpdatesTo(this)
        this.list = newlist
    }

    class MessageViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        val txtUser:TextView = itemView.findViewById(R.id.txtUser)
        val txtMessage:TextView = itemView.findViewById(R.id.txtMessage)
    }
}