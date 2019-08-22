package com.antourage.weaverlib.screens.base.chat.rv

import android.support.v7.util.DiffUtil
import com.antourage.weaverlib.other.models.Message

class MessageListDiffUtilCallback(
    private val prevList: List<Message>,
    private val newList: List<Message>
) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        //TODO change to messageId
        return prevList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun getOldListSize(): Int {
        return prevList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return prevList[oldItemPosition].avatarUrl == newList[newItemPosition].avatarUrl &&
                prevList[oldItemPosition].text == newList[newItemPosition].text &&
                prevList[oldItemPosition].nickname == newList[newItemPosition].nickname

    }
}