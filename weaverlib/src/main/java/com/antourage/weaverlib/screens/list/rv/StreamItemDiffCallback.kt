package com.antourage.weaverlib.screens.list.rv

import android.support.v7.util.DiffUtil
import com.antourage.weaverlib.other.models.StreamResponse

class StreamItemDiffCallback(
    private val oldList: List<StreamResponse?>,
    private val newList: List<StreamResponse?>
) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition]?.id == newList[newItemPosition]?.id
    }

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition]?.streamId == newList[newItemPosition]?.streamId
    }
}