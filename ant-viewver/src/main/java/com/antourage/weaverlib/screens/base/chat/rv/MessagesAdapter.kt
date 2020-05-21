package com.antourage.weaverlib.screens.base.chat.rv

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.millisToTime
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.other.models.MessageType

internal class MessagesAdapter(var list: MutableList<Message>, var orientation: Int = Configuration.ORIENTATION_PORTRAIT,
                               private val startTimeLong : Long?) :
    RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>() {
    companion object{
        private const val PORTRAIT_VIEW = 0
        private const val LANDSCAPE_VIEW = 1
    }

    fun changeOrientation(newOrientation: Int){
        orientation = newOrientation
        notifyDataSetChanged()
        //@imurashova: todo :possibly find the way to update only visible items
    }

    override fun getItemViewType(position: Int): Int {
        return if (orientation == Configuration.ORIENTATION_PORTRAIT) PORTRAIT_VIEW else LANDSCAPE_VIEW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MessageViewHolder(
        LayoutInflater.from(parent.context).inflate(
            if (viewType == PORTRAIT_VIEW) {
                R.layout.item_message_portrait
            } else{
                R.layout.item_message_landscape
            }, parent, false
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

    fun setMessageList(newList: List<Message>, isFiltered: Boolean = false) {
        val userMessagesList = if (isFiltered) {
            newList
        } else {
            newList.filter { it.type == MessageType.USER }
        }
        val diffResult = DiffUtil.calculateDiff(MessageListDiffUtilCallback(list, userMessagesList))

        this.list.clear()
        //use this instead of addAll, as it will create copy without reference to prev. objects
        this.list = ArrayList(userMessagesList.map { it.copy().apply { id = it.id } })

        diffResult.dispatchUpdatesTo(this)
    }

    fun amountOfNewMessagesWillBeAdd(list: List<Message>): Int = list.size - this.list.size

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtUser: TextView = itemView.findViewById(R.id.txtUser)
        val txtMessage: TextView = itemView.findViewById(R.id.txtMessage)
        val txtTimeAdded: TextView = itemView.findViewById(R.id.txtTimeAt)
    }
}