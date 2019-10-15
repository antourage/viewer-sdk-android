package com.antourage.weaverlib.screens.list

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.antourage.weaverlib.R

class VideoPlaceholdersAdapter :
    RecyclerView.Adapter<VideoPlaceholdersAdapter.PlaceholderViewHolder>() {
    private var listOfItems = ArrayList<@android.support.annotation.DrawableRes Int>(3)

    fun setItems(newItems: ArrayList<Int>) {
        this.listOfItems.clear()
        this.listOfItems.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceholderViewHolder {
        return PlaceholderViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_video_placeholder,
                parent,
                false
            )
        )
    }

    override fun getItemCount() = listOfItems.size

    override fun onBindViewHolder(p0: PlaceholderViewHolder, p1: Int) {
        p0.placeholderImg.setImageResource(listOfItems[p1])
    }

    class PlaceholderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val placeholderImg: ImageView = itemView.findViewById(R.id.placeholderImg)
    }
}