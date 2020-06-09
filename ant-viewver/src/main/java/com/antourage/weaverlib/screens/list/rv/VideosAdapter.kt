package com.antourage.weaverlib.screens.list.rv

import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.*
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.screens.list.rv.StreamItemDiffCallback.Companion.REFRESH_DURATION
import com.antourage.weaverlib.screens.list.rv.StreamItemDiffCallback.Companion.REFRESH_LIVE
import com.antourage.weaverlib.screens.list.rv.StreamItemDiffCallback.Companion.REFRESH_NICKNAME
import com.antourage.weaverlib.screens.list.rv.StreamItemDiffCallback.Companion.REFRESH_TIME
import com.antourage.weaverlib.screens.list.rv.StreamItemDiffCallback.Companion.REFRESH_VOD
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.item_jump_to_top.view.*
import kotlinx.android.synthetic.main.item_live_video.view.*
import kotlinx.android.synthetic.main.item_progress.view.*
import kotlinx.android.synthetic.main.item_vod.view.*
import org.jetbrains.anko.windowManager
import java.util.*

internal class VideosAdapter(
    private val onClick: (stream: StreamResponse) -> Unit,
    private val onJoinClicked: (stream: StreamResponse) -> Unit,
    private val recyclerView: VideoPlayerRecyclerView
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var listOfStreams: MutableList<StreamResponse> = mutableListOf()
    lateinit var context: Context
    private var isListInitial = true
    private var calculatedHeight = 0
    private var screenWidth = 0

    companion object {
        const val VIEW_LIVE: Int = 0
        const val VIEW_VOD: Int = 1
        const val VIEW_JUMP_TO_TOP = 2
        const val VIEW_PROGRESS: Int = 3
    }

    fun setStreamList(newListOfStreams: List<StreamResponse>) {
        val diffCallback = StreamItemDiffCallback(this.listOfStreams, newListOfStreams)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        recyclerView.setMediaObjects(listOfStreams as ArrayList<StreamResponse>)

        diffResult.dispatchUpdatesTo(this)

        this.listOfStreams.clear()
        this.listOfStreams.addAll(newListOfStreams)

        if (isListInitial && listOfStreams.isNotEmpty()) {
            recyclerView.afterMeasured {
                this.playVideo()
            }
            isListInitial = false
        }

    }

    fun setStreamListForceUpdate(newListOfStreams: List<StreamResponse>) {
        this.listOfStreams.clear()
        this.listOfStreams.addAll(newListOfStreams)
        recyclerView.setMediaObjects(listOfStreams as ArrayList<StreamResponse>)
        notifyDataSetChanged()
    }

    fun getStreams() = listOfStreams

    private fun setThumbnailsSize(views: List<View>) {
        if (calculatedHeight == 0) calculatedHeight = getHeightNeeded()
        val constraintSet = ConstraintSet()
        constraintSet.clone(views[0].parent as ConstraintLayout)
        for (view in views) {
            constraintSet.constrainWidth(view.id, screenWidth)
            constraintSet.constrainHeight(view.id, calculatedHeight)
        }
        constraintSet.applyTo(views[0].parent as ConstraintLayout)
    }

    private fun getHeightNeeded(): Int {
        val displayMetrics = DisplayMetrics()
        context.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels
        screenWidth = width
        return width / 4 * 3
    }


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
                when (key) {
                    REFRESH_VOD -> {
                        if (holder is VODViewHolder) {
                            holder.setViews(bundle.getLong(key).toString())
                        }
                    }
                    REFRESH_LIVE -> {
                        if (holder is LiveVideoViewHolder) {
                            holder.setViews(bundle.getInt(key).toString())
                        }
                    }
                    REFRESH_TIME -> {
                        if (holder is LiveVideoViewHolder) {
                            holder.setTime(bundle.getString(key), bundle.getString(REFRESH_NICKNAME))
                        }else if(holder is VODViewHolder){
                            holder.setTime(bundle.getString(key), bundle.getString(REFRESH_NICKNAME), bundle.getString(REFRESH_DURATION))
                        }
                    }
                }
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (position < itemCount) {
            if (listOfStreams[position].id == -1) {
                return VIEW_JUMP_TO_TOP
            }
            if (listOfStreams[position].id == -2) {
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
        ivThumbnail?.alpha = 0f
        ivPlaceholder.alpha = 1f

        val picasso = if (!thumbnailUrl.isNullOrBlank()) Picasso.get()
            .load(thumbnailUrl)
        else
            Picasso.get()
                .load(R.drawable.antourage_ic_placeholder_video)

        //needed, because in other way placeholder gets distorted when switching to thumbnail
        picasso.into(ivThumbnail, object : Callback {
            override fun onSuccess() {
                ivThumbnail?.alpha = 1f
                ivPlaceholder.alpha = 0f
            }

            override fun onError(e: Exception?) {}
        })
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

        fun setViews(views: String) {
            itemView.txtViewersCount_live.text = views
        }

        fun setTime(startTime: String?, creatorNickname: String?) {
            val formattedStartTime = startTime?.parseDateLong(context)
            val streamerNameAndTime = "$creatorNickname  •  $formattedStartTime"
            itemView.txtStreamerInfo_live.text = streamerNameAndTime
            itemView.txtStreamerInfo_live.visible(!formattedStartTime.isNullOrEmpty())
        }

        fun bindView(liveStream: StreamResponse) {
            parent.tag = this
            with(itemView) {
                liveStream.apply {
                    setThumbnailsSize(
                        listOf(
                            ivThumbnail_live,
                            ivThumbnail_live_placeholder,
                            mediaContainer_live
                        )
                    )
                    loadThumbnailUrlOrShowPlaceholder(
                        thumbnailUrl,
                        ivThumbnail_live,
                        ivThumbnail_live_placeholder
                    )

                    loadStreamerImageOrShowPlaceholder(broadcasterPicUrl, ivStreamerPicture_live)

                    isChatEnabled?.let {
                        if (!it && lastMessage.isNullOrEmpty()) {
                            btnChat_live.gone(true)
                        } else {
                            btnChat_live.visibility = View.VISIBLE
                        }
                    }
                    isChatEnabled?.let {
                        if (it) {
                            btnJoinConversation.visibility = View.VISIBLE
                        } else {
                            btnJoinConversation.gone(true)
                        }
                    }
                    arePollsEnabled?.let {
                        if (it) {
                            btnPoll_live.visibility = View.VISIBLE
                        } else {
                            btnPoll_live.gone(!it)
                        }
                    }

                    viewButtonsContainer.gone(isChatEnabled != null && isChatEnabled == false && lastMessage.isNullOrEmpty() && arePollsEnabled != null && arePollsEnabled == false)

                    txtComment_live.gone(lastMessage.isNullOrEmpty())
                    txtCommentAuthor_live.gone(lastMessage.isNullOrEmpty())
                    txtComment_live.text = lastMessage
                    val author = resources.getString(R.string.ant_most_recent, lastMessageAuthor)
                    txtCommentAuthor_live.text = author

                    btnJoinConversation.setOnClickListener {
                        if (adapterPosition >= 0
                            && adapterPosition < listOfStreams.size
                            && adapterPosition != -1
                        )
                            listOfStreams[adapterPosition].let {
                                onJoinClicked.invoke(it)
                            }
                    }
                    this@with.setOnClickListener {
                        if (adapterPosition >= 0
                            && adapterPosition < listOfStreams.size
                            && adapterPosition != -1
                        )
                            listOfStreams[adapterPosition].let {
                                onClick.invoke(it)
                            }
                    }
                    txtTitle_live.text = streamTitle
                    txtViewersCount_live.text = viewersCount.toString()
                    txtViewersCount_live.gone(viewersCount == null)
                    val formattedStartTime = startTime?.parseDateLong(context)
                    val streamerNameAndTime = "$creatorNickname  •  $formattedStartTime"
                    txtStreamerInfo_live.text = streamerNameAndTime
                    txtStreamerInfo_live.visible(!formattedStartTime.isNullOrEmpty())
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
        //TODO refactor styles for vod/live item.xml

        var parent: View = itemView

        fun setViews(views: String) {
            itemView.txtViewsCount_vod.text = views
        }

        fun setTime(startTime: String?, creatorNickname: String?, duration: String?) {
            val formattedStartTime =
                duration?.parseToMills()?.plus((startTime?.parseToDate()?.time ?: 0))?.let {
                    Date(it).parseToDisplayAgoTimeLong(context)
                }
            val streamerNameAndTime = "$creatorNickname  •  $formattedStartTime"
            itemView.txtStreamerInfo_vod.text = streamerNameAndTime
            itemView.txtStreamerInfo_vod.visible(!formattedStartTime.isNullOrEmpty())
        }

        fun bindView(vod: StreamResponse) {
            parent.tag = this
            with(itemView) {
                vod.apply {
                    setThumbnailsSize(
                        listOf(
                            ivThumbnail_vod,
                            ivThumbnail_vod_placeholder,
                            mediaContainer_vod,
                            replayContainer
                        )
                    )
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

                    if (recyclerView.fullyViewedVods.contains(listOfStreams[adapterPosition])) {
                        replayContainer.visibility = View.VISIBLE
                    } else {
                        replayContainer.visibility = View.INVISIBLE
                    }

                    isNew?.let { txtNew.gone(!it) }
                    txtTitle_vod.text = videoName
                    txtComment_vod.gone(lastMessage.isNullOrEmpty())
                    txtCommentAuthor_vod.gone(lastMessage.isNullOrEmpty())
                    btnChat_vod.gone(lastMessage.isNullOrEmpty())
                    txtComment_vod.text = lastMessage
                    val author = resources.getString(R.string.ant_most_recent, lastMessageAuthor)
                    txtCommentAuthor_vod.text = author


                    val formattedStartTime =
                        duration?.parseToMills()?.plus((startTime?.parseToDate()?.time ?: 0))?.let {
                            Date(it).parseToDisplayAgoTimeLong(context)
                        }
                    val streamerNameAndTime = "$creatorNickname  •  $formattedStartTime"
                    txtStreamerInfo_vod.text = streamerNameAndTime
                    txtStreamerInfo_vod.visible(!formattedStartTime.isNullOrEmpty())
                    txtDuration_vod.text = duration?.parseToMills()?.millisToTime()
                    txtDuration_vod.gone(duration == null || duration.isEmpty())
                    txtViewsCount_vod.text = viewsCount.toString()
                    txtViewsCount_vod.gone(viewsCount == null)

                    if (stopTimeMillis != 0L) {
                        watchingProgress.progress =
                            ((stopTimeMillis * 100) / (duration?.parseToMills()
                                ?: 1)).toInt()
                        watchingProgress.visibility = View.VISIBLE
                    } else {
                        watchingProgress.progress = 0
                        watchingProgress.visibility = View.INVISIBLE
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
                ivProgressLoadMore.clearAnimation()
                ivProgressLoadMore.setHasTransientState(true)
                rotateAnimation.setAnimationListener(
                    (object : AnimatorListenerAdapter(), Animation.AnimationListener {
                        override fun onAnimationRepeat(p0: Animation?) {
                        }

                        override fun onAnimationEnd(p0: Animation?) {
                            ivProgressLoadMore.setHasTransientState(false)
                        }

                        override fun onAnimationStart(p0: Animation?) {
                        }
                    })
                )
                ivProgressLoadMore.startAnimation(rotateAnimation)
            }
        }

        fun cleanup() {
            with(itemView) {
                ivProgressLoadMore.clearAnimation()
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