package com.antourage.weaverlib.screens.list.rv

import android.content.Context
import android.os.Bundle
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.*
import com.antourage.weaverlib.other.gone
import com.antourage.weaverlib.other.isEmptyTrimmed
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.parseDate
import com.antourage.weaverlib.other.parseToMills
import com.antourage.weaverlib.screens.list.rv.StreamListDiffCallback.Companion.ARGS_REFRESH_TIMESTAMP
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.item_live_video.view.*
import kotlinx.android.synthetic.main.item_vod.view.*
import java.util.*

internal class VideosAdapter(private val onClick: (stream: StreamResponse) -> Unit) :
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
            VIEW_VOD -> return VODViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_vod,
                    parent,
                    false
                )
            )
            VIEW_LIVE -> return LiveVideoViewHolder(
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

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        when (holder) {
            is LiveVideoViewHolder -> holder.cleanup()
            is VODViewHolder -> holder.cleanup()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is LiveVideoViewHolder -> holder.bindView(listOfStreams[position])
            is VODViewHolder -> holder.bindView(listOfStreams[position])
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.size > 0 && payloads[0] is Bundle) {
            if ((payloads[0] as Bundle).getBoolean(ARGS_REFRESH_TIMESTAMP, false)) {
                if (holder is LiveVideoViewHolder) {
                    holder.itemView.txtViewersCount.text =
                        listOfStreams[position].viewsCount.toString()
                } else if (holder is VODViewHolder) {
                    holder.itemView.txtViewsCount.text =
                        listOfStreams[position].viewsCount.toString()
                }
            }
        }
        super.onBindViewHolder(holder, position, payloads)
    }

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

    inner class LiveVideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindView(liveStream: StreamResponse) {
            with(itemView) {
                liveStream.apply {
                    if (!thumbnailUrl.isNullOrEmpty()) {
                        Picasso.get()
                            .load(thumbnailUrl)
                            .placeholder(R.drawable.antourage_ic_no_content_content_loading)
                            .error(R.drawable.antourage_ic_no_content_content_loading)
                            .into(ivThumbnail_live)
                    }
                    this@with.setOnClickListener {
                        if (adapterPosition >= 0
                            && adapterPosition < listOfStreams.size
                            && adapterPosition != -1
                        )
                            listOfStreams[adapterPosition].let { onClick.invoke(it) }
                    }
                    txtTitle_live.text = streamTitle
                    txtViewersCount.text = viewersCount.toString()
                    txtViewersCount.gone(viewersCount == null)
                    val formattedStartTime = startTime?.parseDate(context)
                    txtWasLive_live.text = formattedStartTime
                    txtWasLive_live.gone(formattedStartTime.isNullOrEmpty())
                }
            }
        }

        fun cleanup() {
            with(itemView) {
                Picasso.get().cancelRequest(this.ivThumbnail_live)
            }
        }
    }

    inner class VODViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindView(vod: StreamResponse) {
            with(itemView) {
                vod.apply {
                    if (!thumbnailUrl.isNullOrEmpty()) {
                        Picasso.get()
                            .load(thumbnailUrl)
                            .placeholder(R.drawable.antourage_ic_no_content_content_loading)
                            .error(R.drawable.antourage_ic_no_content_content_loading)
                            .into(ivThumbnail_vod)
                    }
                    this@with.setOnClickListener {
                        if (adapterPosition >= 0 && adapterPosition < listOfStreams.size &&
                            adapterPosition != -1
                        )
                            listOfStreams[adapterPosition].let { onClick.invoke(it) }
                    }
                    isNew?.let { txtNew.gone(!it) }
                    txtTitle_vod.text = videoName
                    val formattedStartTime =
                        duration?.parseToMills()?.plus((startTime?.parseToDate()?.time ?: 0))?.let {
                            Date(it).parseToDisplayAgoTime(context)
                        }
                    txtWasLive_vod.text = formattedStartTime
                    txtWasLive_vod.gone(formattedStartTime.isNullOrEmpty())
                    txtDuration.text = duration?.take(8)
                    txtDuration.gone(duration == null || duration.isEmpty())
                    txtViewsCount.text = viewsCount.toString()
                    txtViewsCount.gone(viewsCount == null)
                    if (stopTime != null && (stopTime?.isEmptyTrimmed() == false) && !stopTime.equals(
                            "00:00:00"
                        )
                    ) {
                        watchingProgress.progress =
                            (((stopTime?.parseToMills() ?: 0) * 100) / (duration?.parseToMills()
                                ?: 0)).toInt()
                        watchingProgress.visibility = View.VISIBLE
                    }
                }
            }
        }

        fun cleanup() {
            with(itemView) {
                Picasso.get().cancelRequest(this.ivThumbnail_vod)
            }
        }
    }

    inner class ProgressHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class SeparatorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}