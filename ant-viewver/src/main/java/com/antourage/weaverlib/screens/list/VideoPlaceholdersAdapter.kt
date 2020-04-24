package com.antourage.weaverlib.screens.list

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.antourage.weaverlib.R

internal class VideoPlaceholdersAdapter :
    RecyclerView.Adapter<VideoPlaceholdersAdapter.PlaceholderViewHolder>() {
    private var listOfItems = ArrayList<@ColorRes Int>(3)
    lateinit var context: Context

    fun setItems(newItems: ArrayList<Int>) {
        this.listOfItems.clear()
        this.listOfItems.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceholderViewHolder {
        context = parent.context
        return PlaceholderViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_video_placeholder,
//                R.layout.item_live_video2,
                parent,
                false
            )
        )
    }

    override fun getItemCount() = listOfItems.size

    override fun onBindViewHolder(p0: PlaceholderViewHolder, p1: Int) {
    }

    class PlaceholderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    }
}