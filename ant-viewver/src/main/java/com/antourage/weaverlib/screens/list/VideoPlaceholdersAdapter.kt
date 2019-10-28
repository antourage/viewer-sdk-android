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
    private var oldItems = ArrayList<@ColorRes Int>(3)
    private var orderedItems = arrayListOf(
        R.color.ant_no_content_placeholder_color_1,
        R.color.ant_no_content_placeholder_color_2,
        R.color.ant_no_content_placeholder_color_3
    )
    private var shiftCount = 0
    lateinit var context: Context

    fun setItems(newItems: ArrayList<Int>) {
        this.listOfItems.clear()
        this.listOfItems.addAll(newItems)
        this.oldItems.clear()
        this.oldItems.addAll(newItems)
        notifyDataSetChanged()
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

    override fun onBindViewHolder(p0: PlaceholderViewHolder, p1: Int) {
        changeColorWithAnim(oldItems[p1], listOfItems[p1], p0.cardView)
    }

    fun shiftItems() {
        this.oldItems.clear()
        this.oldItems.addAll(listOfItems)
        when (shiftCount) {
            0 -> {
                listOfItems[0] = orderedItems[2]
                listOfItems[1] = orderedItems[1]
                listOfItems[2] = orderedItems[0]
            }
            1 -> {
                listOfItems[0] = orderedItems[0]
                listOfItems[1] = orderedItems[2]
                listOfItems[2] = orderedItems[1]
            }
            2 -> {
                listOfItems[0] = orderedItems[0]
                listOfItems[1] = orderedItems[1]
                listOfItems[2] = orderedItems[2]
            }
        }
        shiftCount++
        if (shiftCount > 2) shiftCount = 0
        notifyDataSetChanged()
    }

    class PlaceholderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.rootCardView)
    }

    private fun changeColorWithAnim(colorFrom: Int, colorTo: Int, cardView: CardView) {
        val from = ContextCompat.getColor(context, colorFrom)
        val to = ContextCompat.getColor(context, colorTo)
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), from, to)
        colorAnimation.duration = 350
        colorAnimation.addUpdateListener { animator -> cardView.setCardBackgroundColor(animator.animatedValue as Int) }
        colorAnimation.start()
    }
}