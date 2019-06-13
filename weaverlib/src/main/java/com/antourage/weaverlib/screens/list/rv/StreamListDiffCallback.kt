package com.antourage.weaverlib.screens.list.rv

import android.os.Bundle
import android.support.v7.util.DiffUtil
import com.antourage.weaverlib.other.models.StreamResponse

class StreamListDiffCallback(
    private val prevList: MutableList<StreamResponse?>,
    private val newList: List<StreamResponse?>
) : DiffUtil.Callback() {

    companion object{
       const val ARGS_REFRESH_TIMESTAMP = "refresh_timestamp"
    }

    override fun getOldListSize(): Int {
        return prevList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
        return prevList[oldPos]?.streamId == newList[newPos]?.streamId
    }

    override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
        return prevList[oldPos]?.hlsUrl == newList[newPos]?.hlsUrl &&
                prevList[oldPos]?.thumbnailUrl == newList[newPos]?.thumbnailUrl &&
                prevList[oldPos]?.streamTitle == newList[newPos]?.streamTitle
    }

    override fun getChangePayload(oldPos: Int, newPos: Int): Any? {
        val bundle = Bundle()
        bundle.putBoolean(ARGS_REFRESH_TIMESTAMP, true)
        return bundle
    }
}