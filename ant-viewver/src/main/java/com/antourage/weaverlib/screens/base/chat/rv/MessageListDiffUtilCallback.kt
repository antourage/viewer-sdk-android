package com.antourage.weaverlib.screens.base.chat.rv

import android.support.v7.util.DiffUtil
import com.antourage.weaverlib.other.models.Message

internal class MessageListDiffUtilCallback(
    private val prevList: List<Message>,
    private val newList: List<Message>
) : DiffUtil.Callback() {
    //TODO change to messageId
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        prevList[oldItemPosition].id == newList[newItemPosition].id

    override fun getOldListSize() = prevList.size

    override fun getNewListSize() = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        prevList[oldItemPosition] == newList[newItemPosition]
}