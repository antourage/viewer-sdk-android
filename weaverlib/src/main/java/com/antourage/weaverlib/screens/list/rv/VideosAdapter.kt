package com.antourage.weaverlib.screens.list.rv

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.gone
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.parseDate
import com.antourage.weaverlib.screens.list.rv.StreamListDiffCallback.Companion.ARGS_REFRESH_TIMESTAMP
import com.squareup.picasso.Picasso

class VideosAdapter(private val onClick: (stream: StreamResponse) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var listOfStreams: MutableList<StreamResponse?> = mutableListOf()
    lateinit var context: Context

    companion object {
        const val VIEW_LIVE: Int = 0
        const val VIEW_VOD: Int = 1
        const val VIEW_SEPARATOR = 2
    }

    fun setStreamList(list: List<StreamResponse>) {
        listOfStreams.clear()
        listOfStreams.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        when (viewType) {
            VIEW_VOD -> return VideoViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_video,
                    parent,
                    false
                )
            )
            VIEW_LIVE -> return VideoViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_broadcast,
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
            videoItem?.apply {
                Picasso.get().load(thumbnailUrl).into(holder.thumbnail)
                holder.itemView.setOnClickListener {
                    if (holder.adapterPosition >= 0 && holder.adapterPosition < listOfStreams.size &&
                        holder.adapterPosition != -1
                    )
                        listOfStreams[holder.adapterPosition]?.let { onClick.invoke(it) }
                }

                holder.txtNumberOfViewers.text = viewerCounter.toString()
                holder.txtNumberOfViewers.gone(viewerCounter == null)

                val formattedStartTime = startTime?.parseDate(context)
                holder.txtWasLive.text = formattedStartTime
                holder.txtWasLive.gone(formattedStartTime.isNullOrEmpty())

                when (getItemViewType(position)) {
                    VIEW_LIVE -> {
                        holder.txtTitle.text = streamTitle
                    }
                    VIEW_VOD -> {
                        holder.txtTitle.text = videoName
                        holder.txtStatus.text = duration
                        holder.txtStatus.gone(duration == null || duration.isEmpty())
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
                        listOfStreams[position]?.viewerCounter.toString()
                    holder.txtWasLive.text = listOfStreams[position]?.startTime?.parseDate(context)
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
    }

    class SeparatorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun getItemViewType(position: Int): Int {
        if (position < itemCount) {
            if (listOfStreams[position]?.id == -1) {
                return VIEW_SEPARATOR
            }
            if (listOfStreams[position]?.isLive == true) {
                return VIEW_LIVE
            } else {
                return VIEW_VOD
            }
        } else
            return VIEW_VOD
    }
}