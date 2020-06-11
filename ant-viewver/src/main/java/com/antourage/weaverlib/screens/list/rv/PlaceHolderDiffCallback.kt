package com.antourage.weaverlib.screens.list.rv

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil

internal class PlaceHolderDiffCallback(
    private val oldList: List<Int>,
    private val newList: List<Int>
) : DiffUtil.Callback() {

    companion object {
        const val STATE = "state"
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItemPosition == newItemPosition
    }

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldState = oldList[oldItemPosition]
        val newState = newList[newItemPosition]

        val diffBundle = Bundle()

        if (newState != oldState) {
            diffBundle.putInt(STATE, newState)
        }

        if (diffBundle.isEmpty) return null;
        return diffBundle
    }
}