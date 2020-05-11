package com.antourage.weaverlib.screens.list

import android.content.Context
import android.util.DisplayMetrics
import androidx.annotation.ColorRes
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.antourage.weaverlib.R
import kotlinx.android.synthetic.main.item_video_placeholder.view.*
import org.jetbrains.anko.windowManager

internal class VideoPlaceholdersAdapter :
    RecyclerView.Adapter<VideoPlaceholdersAdapter.PlaceholderViewHolder>() {
    private var listOfItems = ArrayList<@ColorRes Int>(3)
    lateinit var context: Context
    private var calculatedHeight = 0
    private var screenWidth = 0

    fun setItems(newItems: ArrayList<Int>) {
        this.listOfItems.clear()
        this.listOfItems.addAll(newItems)
        notifyDataSetChanged()
    }

    private fun setThumbnailSize(view: View) {
        if (calculatedHeight == 0) calculatedHeight = getHeightNeeded()
        val constraintSet = ConstraintSet()
        constraintSet.clone(view.parent as ConstraintLayout)
        constraintSet.constrainWidth(view.id,screenWidth)
        constraintSet.constrainHeight(view.id,calculatedHeight)
        constraintSet.applyTo(view.parent as ConstraintLayout)
    }

    private fun getHeightNeeded(): Int {
        val displayMetrics = DisplayMetrics()
        context.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels
        screenWidth = width
        return width / 4 * 3
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceholderViewHolder {
        context = parent.context
        return PlaceholderViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_video_placeholder,
                parent,
                false
            )
        )
    }


    override fun getItemCount() = listOfItems.size

    override fun onBindViewHolder(holder: PlaceholderViewHolder, p1: Int) {
        holder.bindView()
    }

    inner class PlaceholderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindView() {
            with(itemView){
                setThumbnailSize(ivThumbnail_holder)
            }
        }
    }
}