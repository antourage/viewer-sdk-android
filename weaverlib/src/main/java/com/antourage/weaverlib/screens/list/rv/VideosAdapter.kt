package com.antourage.weaverlib.screens.list.rv

import android.content.Context
import android.os.Bundle
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.antourage.weaverlib.R
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

        val tempList: MutableList<StreamResponse?> = list.toMutableList()
        val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(
            StreamListDiffCallback(listOfStreams, tempList)
        )
        diffResult.dispatchUpdatesTo(this)
        this.listOfStreams = tempList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        if (viewType == VIEW_VOD)
            return VideoViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_video,
                    parent,
                    false
                )
            )
        else if (viewType == VIEW_LIVE)
            return VideoViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_broadcast,
                    parent,
                    false
                )
            )
        else
            return SeparatorViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_separator,
                    parent,
                    false
                )
            )
    }

    override fun getItemCount(): Int {
        return listOfStreams.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is VideoViewHolder) {
            Picasso.get().load(listOfStreams[position]?.thumbnailUrl).into(holder.thumbnail)
            holder.txtTitle.text = listOfStreams[position]?.streamTitle
            if (listOfStreams[position]?.duration != null && listOfStreams[position]?.duration != 0)
                holder.txtStatus.text = ("0:" + listOfStreams[position]?.duration)
            holder.itemView.setOnClickListener {
                if (holder.adapterPosition >= 0 && holder.adapterPosition < listOfStreams.size &&
                    holder.adapterPosition != -1
                )
                    listOfStreams[holder.adapterPosition]?.let { onClick.invoke(it) }
            }
            holder.txtNumberOfViewers.text = listOfStreams[position]?.viewerCounter.toString()
            holder.txtWasLive.text = listOfStreams[position]?.startTime?.parseDate(context)
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
            if (listOfStreams[position]?.streamId == -1) {
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