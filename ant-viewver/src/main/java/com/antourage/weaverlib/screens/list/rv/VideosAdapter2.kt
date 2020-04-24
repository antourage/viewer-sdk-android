package com.antourage.weaverlib.screens.list.rv

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.constraintlayout.widget.Placeholder
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.*
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.screens.list.rv.StreamItemDiffCallback.Companion.ARGS_REFRESH_VIEWS
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.item_jump_to_top.view.*
import kotlinx.android.synthetic.main.item_live_video2.view.*
import kotlinx.android.synthetic.main.item_progress.view.*
import kotlinx.android.synthetic.main.item_vod2.view.*
import java.util.*

internal class VideosAdapter2(private val onClick: (stream: StreamResponse) -> Unit, private val recyclerView: VideoPlayerRecyclerView) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var listOfStreams: MutableList<StreamResponse> = mutableListOf()
    lateinit var context: Context

    companion object {
        const val VIEW_LIVE: Int = 0
        const val VIEW_VOD: Int = 1
        const val VIEW_JUMP_TO_TOP = 2
        const val VIEW_PROGRESS: Int = 3
    }

    fun setStreamList(newListOfStreams: List<StreamResponse>) {
        val diffCallback = StreamItemDiffCallback(this.listOfStreams, newListOfStreams)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.listOfStreams.clear()
        this.listOfStreams.addAll(newListOfStreams)
        recyclerView.setMediaObjects(listOfStreams as ArrayList<StreamResponse>)
        diffResult.dispatchUpdatesTo(this)
    }

    fun setStreamListForceUpdate(newListOfStreams: List<StreamResponse>) {
        this.listOfStreams.clear()
        this.listOfStreams.addAll(newListOfStreams)
        notifyDataSetChanged()
    }

    fun getStreams() = listOfStreams

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        when (viewType) {
            VIEW_VOD -> return VODViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_vod2,
                    parent,
                    false
                )
            )
            VIEW_LIVE -> return LiveVideoViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_live_video2,
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
            else -> return JumpToTopHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_jump_to_top,
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
            is ProgressHolder -> holder.cleanup()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is LiveVideoViewHolder -> holder.bindView(listOfStreams[position])
            is VODViewHolder -> holder.bindView(listOfStreams[position])
            is ProgressHolder -> holder.bindView()
            is JumpToTopHolder -> holder.bindView(listOfStreams[position])
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            val bundle = payloads[0] as Bundle
            for (key in bundle.keySet()) {
                if (key == ARGS_REFRESH_VIEWS) {
                    if (holder is LiveVideoViewHolder) {
                        holder.itemView.txtViewersCount_live.text =
                            listOfStreams[position].viewsCount.toString()
                    } else if (holder is VODViewHolder) {
                        holder.itemView.txtViewsCount_vod.text =
                            listOfStreams[position].viewsCount.toString()
                    }
                }
            }
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun getItemViewType(position: Int): Int {
        if (position < itemCount) {
            if (listOfStreams[position].streamId == -1) {
                return VIEW_JUMP_TO_TOP
            }
            if (listOfStreams[position].streamId == -2) {
                return VIEW_PROGRESS
            }
            return if (listOfStreams[position].isLive) {
                VIEW_LIVE
            } else {
                VIEW_VOD
            }
        } else
            return VIEW_VOD
    }

    private fun loadThumbnailUrlOrShowPlaceholder(
        thumbnailUrl: String?,
        ivThumbnail: ImageView?,
        ivPlaceholder: ImageView
    ) {

        val picasso = if (!thumbnailUrl.isNullOrBlank()) Picasso.get()
            .load(thumbnailUrl)
        else
            Picasso.get()
                .load(R.drawable.antourage_ic_placeholder_video)

        picasso.into(ivThumbnail, object : Callback {
            override fun onSuccess() {
                ivPlaceholder.animate()?.setDuration(300)?.alpha(0f)?.start()
                ivThumbnail?.alpha = 0f
                ivThumbnail?.animate()?.setDuration(300)?.alpha(1f)?.start()
            }

            override fun onError(e: Exception?) {}
        })


//        picasso
//            .fit()
//            .centerCrop()
////            .noFade()
//            .placeholder(R.drawable.antourage_ic_placeholder_video)
//            .error(R.drawable.antourage_ic_placeholder_video)
//            .into(ivThumbnail)
    }

    private fun loadStreamerImageOrShowPlaceholder(imageUrl: String?, imageView: ImageView?) {
        val picasso = if (!imageUrl.isNullOrBlank()) Picasso.get()
            .load(imageUrl)
        else
            Picasso.get()
                .load(R.drawable.antourage_ic_default_user)
        picasso
            .placeholder(R.drawable.antourage_ic_default_user)
            .error(R.drawable.antourage_ic_default_user)
            .into(imageView)
    }

    inner class LiveVideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var parent: View = itemView
        fun bindView(liveStream: StreamResponse) {
            parent.tag = this
            with(itemView) {
                liveStream.apply {
                    loadThumbnailUrlOrShowPlaceholder(
                        thumbnailUrl,
                        ivThumbnail_live,
                        ivThumbnail_live_placeholder
                    )
                    loadStreamerImageOrShowPlaceholder(broadcasterPicUrl, ivStreamerPicture_live)
                    this@with.setOnClickListener {
                        if (adapterPosition >= 0
                            && adapterPosition < listOfStreams.size
                            && adapterPosition != -1
                        )
                            listOfStreams[adapterPosition].let { onClick.invoke(it) }
                    }
                    txtTitle_live.text = streamTitle
                    txtViewersCount_live.text = viewersCount.toString()
                    txtViewersCount_live.gone(viewersCount == null)
                    val formattedStartTime = startTime?.parseDate(context)
                    val streamerNameAndTime = "$creatorFullName  •  $formattedStartTime"
                    txtStreamerInfo_live.text = streamerNameAndTime
                    txtStreamerInfo_live.gone(formattedStartTime.isNullOrEmpty())
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
        var parent: View = itemView
        fun bindView(vod: StreamResponse) {
            parent.tag = this
            with(itemView) {
                vod.apply {
                    loadThumbnailUrlOrShowPlaceholder(
                        thumbnailUrl,
                        ivThumbnail_vod,
                        ivThumbnail_vod_placeholder
                    )
                    loadStreamerImageOrShowPlaceholder(broadcasterPicUrl, ivStreamerPicture_vod)
                    this@with.setOnClickListener {
                        if (adapterPosition >= 0 && adapterPosition < listOfStreams.size &&
                            adapterPosition != -1
                        )
                            listOfStreams[adapterPosition].let { onClick.invoke(it) }
                    }
                    isNew?.let { txtNew.gone(!it) }
                    txtTitle_vod.text = videoName

                    if (adapterPosition % 2 == 0) {
                        txtComment_vod.text = "Love this sport"
                    } else {
                        txtComment_vod.text =
                            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque lectus risus, commodo ac convallis eu, lacinia quis neque. Aliquam malesuada, eros eget consequat tincidunt, lorem magna molestie nulla, ac gravida lectus nibh vitae metus. Nam mi urna, rutrum in consectetur in, volutpat in felis. Etiam dictum odio id erat gravida, a posuere augue condimentum. Nullam consectetur interdum dictum. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam ultricies lectus odio, eu lacinia massa condimentum at. Quisque sed massa quam. Nullam vel lobortis nisi."
                    }

                    val formattedStartTime =
                        duration?.parseToMills()?.plus((startTime?.parseToDate()?.time ?: 0))?.let {
                            Date(it).parseToDisplayAgoTime(context)
                        }
                    val streamerNameAndTime = "$creatorFullName  •  $formattedStartTime"
                    txtStreamerInfo_vod.text = streamerNameAndTime
                    txtStreamerInfo_vod.gone(formattedStartTime.isNullOrEmpty())
                    txtDuration_vod.text = duration?.take(8)
                    txtDuration_vod.gone(duration == null || duration.isEmpty())
                    txtViewsCount_vod.text = viewsCount.toString()
                    txtViewsCount_vod.gone(viewsCount == null)
                    if (stopTime != null && (stopTime?.isEmptyTrimmed() == false) && !stopTime.equals(
                            "00:00:00"
                        )
                    ) {
                        watchingProgress.progress =
                            (((stopTime?.parseToMills() ?: 0) * 100) / (duration?.parseToMills()
                                ?: 1)).toInt()
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

    inner class ProgressHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rotateAnimation: Animation =
            AnimationUtils.loadAnimation(context, R.anim.antourage_rotate_anim)

        fun bindView() {
            with(itemView) {
                ivProgressLoadMore.startAnimation(rotateAnimation)
            }
        }

        fun cleanup() {
            with(itemView) {
                ivProgressLoadMore.clearAnimation()
                rotateAnimation.cancel()
            }
        }
    }

    inner class JumpToTopHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindView(vod: StreamResponse) {
            with(itemView) {
                vod.apply {
                    btnJumpToTop.setOnClickListener {
                        if (adapterPosition >= 0
                            && adapterPosition < listOfStreams.size
                            && adapterPosition != -1
                        )
                            listOfStreams[adapterPosition].let { onClick.invoke(this) }
                    }
                }
            }
        }
    }
}