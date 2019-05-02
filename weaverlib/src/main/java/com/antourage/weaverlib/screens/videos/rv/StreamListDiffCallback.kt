package com.antourage.weaverlib.screens.videos.rv

import androidx.recyclerview.widget.DiffUtil
import com.antourage.weaverlib.other.networking.models.StreamResponse

class StreamListDiffCallback(
    private val prevList: MutableList<StreamResponse?>,
    private val newList: List<StreamResponse?>
) : DiffUtil.Callback() {

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

//    override fun getChangePayload(oldPos: Int, newPos: Int): Any? {
//        val bundle = Bundle()
//        if (prevList[oldPos]?.isFavourite != newList[newPos]?.isFavourite)
//            bundle.putBoolean(ARGS_FAVOURITE, newList[newPos]?.isFavourite!!)
//        return bundle
//    }
}