package com.antourage.weaverlib.screens.list.rv

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import com.antourage.weaverlib.other.models.StreamResponse


internal class StreamItemDiffCallback(
    private val oldList: List<StreamResponse>,
    private val newList: List<StreamResponse>
) : DiffUtil.Callback() {

    companion object {
        const val ARGS_REFRESH_VIEWS = "refresh_views"
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
//        return oldList[oldItemPosition].viewersCount == newList[newItemPosition].viewersCount &&
                 return  oldList[oldItemPosition].viewsCount == newList[newItemPosition].viewsCount &&
                oldList[oldItemPosition].isNew == newList[newItemPosition].isNew &&
                oldList[oldItemPosition].startTime.equals(newList[newItemPosition].startTime)
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldStream = oldList[oldItemPosition]
        val newStream = newList[newItemPosition]
        val diffBundle = Bundle()

        if (newStream.viewsCount != oldStream.viewsCount) {
            newStream.viewsCount?.let { diffBundle.putInt(ARGS_REFRESH_VIEWS, it) }
        }
        if (diffBundle.isEmpty) return null;
        return diffBundle
    }
}