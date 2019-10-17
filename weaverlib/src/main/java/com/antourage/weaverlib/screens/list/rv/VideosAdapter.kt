package com.antourage.weaverlib.screens.list.rv

import android.content.Context
import android.os.Bundle
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.gone
import com.antourage.weaverlib.other.isEmptyTrimmed
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.parseDate
import com.antourage.weaverlib.other.parseToMills
import com.antourage.weaverlib.screens.list.rv.StreamListDiffCallback.Companion.ARGS_REFRESH_TIMESTAMP
import com.squareup.picasso.Picasso

class VideosAdapter(private val onClick: (stream: StreamResponse) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var listOfStreams: MutableList<StreamResponse> = mutableListOf()
    lateinit var context: Context

    companion object {
        const val VIEW_LIVE: Int = 0
        const val VIEW_VOD: Int = 1
        const val VIEW_SEPARATOR = 2
        const val VIEW_PROGRESS: Int = 3
    }

    fun setStreamList(newListOfStreams: List<StreamResponse>) {
        val diffCallback = StreamItemDiffCallback(this.listOfStreams, newListOfStreams)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.listOfStreams.clear()
        this.listOfStreams.addAll(newListOfStreams)
        diffResult.dispatchUpdatesTo(this)
    }

    fun getStreams() = listOfStreams

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        when (viewType) {
            VIEW_VOD -> return VideoViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_vod,
                    parent,
                    false
                )
            )
            VIEW_LIVE -> return VideoViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_live_video,
                    parent,
                    false
                )
            )
            VIEW_PROGRESS -> return ProgressHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_progress,
                    parent,
                    false
                )
            )

            else -> return SeparatorViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_separator,
                    parent,
                    false
                )
            )
        }
    }

    override fun getItemCount(): Int {
        return listOfStreams.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is VideoViewHolder) {
            val videoItem = listOfStreams[position]
            videoItem.apply {
                if (!thumbnailUrl.isNullOrEmpty()) {
                    Picasso.get()
                        .load(thumbnailUrl)
                        .placeholder(R.drawable.ic_no_content_content_loading)
                        .error(R.drawable.ic_no_content_content_loading)
                        .into(holder.thumbnail)
                }
                holder.itemView.setOnClickListener {
                    if (holder.adapterPosition >= 0 && holder.adapterPosition < listOfStreams.size &&
                        holder.adapterPosition != -1
                    )
                        listOfStreams[holder.adapterPosition].let { onClick.invoke(it) }
                }

                when (getItemViewType(position)) {
                    VIEW_LIVE -> {
                        holder.txtTitle.text = streamTitle
                        holder.txtNumberOfViewers.text = viewersCount.toString()
                        holder.txtNumberOfViewers.gone(viewersCount == null)
                        val formattedStartTime = startTime?.parseDate(context)
                        holder.txtWasLive.text = formattedStartTime
                        holder.txtWasLive.gone(formattedStartTime.isNullOrEmpty())
                    }
                    VIEW_VOD -> {
                        isNew?.let { holder.txtWasLive.gone(!it) }
                        holder.txtTitle.text = videoName
                        holder.txtStatus.text = duration?.take(8)
                        holder.txtStatus.gone(duration == null || duration.isEmpty())
                        holder.txtNumberOfViewers.text = viewsCount.toString()
                        holder.txtNumberOfViewers.gone(viewsCount == null)
                        if (stopTime != null && (stopTime?.isEmptyTrimmed() == false) && !stopTime.equals(
                                "00:00:00"
                            )
                        ) {
                            holder.watchingProgress.progress =
                                (((stopTime?.parseToMills() ?: 0) * 100) / (duration?.parseToMills()
                                    ?: 0)).toInt()
                            holder.watchingProgress.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.size > 0 && payloads[0] is Bundle) {
            if ((payloads[0] as Bundle).getBoolean(ARGS_REFRESH_TIMESTAMP, false)) {
                if (holder is VideoViewHolder) {
                    holder.txtNumberOfViewers.text =
                        listOfStreams[position].viewsCount.toString()
                }
            }
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnail: ImageView = itemView.findViewById(R.id.thumbnail)
        val txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
        val txtStatus: TextView = itemView.findViewById(R.id.txtStatus)
        val txtNumberOfViewers: TextView = itemView.findViewById(R.id.txtNumberOfViewers)
        val txtWasLive: TextView = itemView.findViewById(R.id.tvWasLive)
        val watchingProgress: ProgressBar = itemView.findViewById(R.id.watchingProgress)
    }

    inner class ProgressHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class SeparatorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun getItemViewType(position: Int): Int {
        if (position < itemCount) {
            if (listOfStreams[position].streamId == -1) {
                return VIEW_SEPARATOR
            }
            if (listOfStreams[position].streamId == -2) {
                return VIEW_PROGRESS
            }
            if (listOfStreams[position].isLive) {
                return VIEW_LIVE
            } else {
                return VIEW_VOD
            }
        } else
            return VIEW_VOD
    }
}