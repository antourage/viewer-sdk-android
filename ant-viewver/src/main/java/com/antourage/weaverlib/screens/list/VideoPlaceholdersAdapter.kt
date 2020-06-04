package com.antourage.weaverlib.screens.list

import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
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
    private var state = ArrayList<Int>(1)
    lateinit var context: Context
    private var calculatedHeight = 0
    private var screenWidth = 0

    enum class LoadingState(val value: Int) {
        LOADING(1),
        NO_INTERNET(2),
        ERROR(3)
    }

    fun setState(newState: LoadingState) {
        if (state.isEmpty() || state[0] != newState.value) setItems(arrayListOf(newState.value))
    }

    fun getState(): Int {
        return if (state.isEmpty()) 0
        else state[0]
    }

    private fun setItems(newItems: ArrayList<Int>) {
        this.state.clear()
        this.state.addAll(newItems)
        notifyDataSetChanged()
    }

    private fun setThumbnailSize(view: View) {
        if (calculatedHeight == 0) calculatedHeight = getHeightNeeded()
        val constraintSet = ConstraintSet()
        constraintSet.clone(view.parent as ConstraintLayout)
        constraintSet.constrainWidth(view.id, screenWidth)
        constraintSet.constrainHeight(view.id, calculatedHeight)
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


    override fun getItemCount() = state.size

    override fun onBindViewHolder(holder: PlaceholderViewHolder, p1: Int) {
        holder.bindView()
    }

    inner class PlaceholderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindView() {
            with(itemView) {
                setThumbnailSize(ivThumbnail_holder)
                if (state.isNotEmpty())
                    when (state[0]) {
                        LoadingState.LOADING.value -> {
                            ivThumbnail_holder.setImageResource(R.drawable.antourage_ic_placeholder_video)
                        }
                        LoadingState.NO_INTERNET.value -> {
                            ivThumbnail_holder.setImageResource(R.drawable.antourage_ic_placeholder_no_connection)
                        }
                        LoadingState.ERROR.value -> {
                            ivThumbnail_holder.setImageResource(R.drawable.antourage_ic_placeholder_error)
                        }
                    }
            }
        }
    }
}