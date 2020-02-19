package com.antourage.weaverlib.screens.list.rv

import androidx.recyclerview.widget.DiffUtil
import com.antourage.weaverlib.other.models.StreamResponse

internal class StreamItemDiffCallback(
    private val oldList: List<StreamResponse>,
    private val newList: List<StreamResponse>
) : DiffUtil.Callback() {

    companion object {
        const val ARGS_REFRESH_TIMESTAMP = "refresh_timestamp"
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].streamId == newList[newItemPosition].streamId &&
                oldList[oldItemPosition].viewersCount == newList[newItemPosition].viewersCount &&
                oldList[oldItemPosition].viewsCount == newList[newItemPosition].viewsCount &&
                oldList[oldItemPosition].isNew == newList[newItemPosition].isNew &&
                oldList[oldItemPosition].startTime.equals(newList[newItemPosition].startTime)
    }
}