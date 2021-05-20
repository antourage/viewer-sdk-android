package com.antourage.weaverlib.screens.list.rv

import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.text.Html
import android.text.Spanned
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
import androidx.viewpager2.widget.ViewPager2
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.*
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.models.StreamResponseType
import com.antourage.weaverlib.screens.list.rv.StreamItemDiffCallback.Companion.REFRESH_LIVE
import com.antourage.weaverlib.screens.list.rv.StreamItemDiffCallback.Companion.REFRESH_VOD
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.item_jump_to_top.view.*
import kotlinx.android.synthetic.main.item_live_video.view.*
import kotlinx.android.synthetic.main.item_post.view.*
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
    private var calculatedHeightForPost = 0
    private var screenWidth = 0

    var viewPageStates: HashMap<Int, Int> = HashMap()

    companion object {
        const val VIEW_LIVE: Int = 0
        const val VIEW_VOD: Int = 1
        const val VIEW_JUMP_TO_TOP = 2
        const val VIEW_PROGRESS: Int = 3
        const val VIEW_POST: Int = 4
    }

    /**format {most recent} in the way it doesn't get split in different lines*/
    private fun formatAuthor(lastMessageAuthor: String, resources: Resources): Spanned {
        val transparentSpace = "<font color='#000000'>_</font>"
        val mostRecentWords = resources.getString(R.string.ant_most_recent).split(" ")
        return if (mostRecentWords.size == 1) {
            (Html.fromHtml("$lastMessageAuthor&nbsp; &nbsp; •$transparentSpace$transparentSpace${mostRecentWords[0]}"))
        } else {
            (Html.fromHtml("$lastMessageAuthor&nbsp; &nbsp; •$transparentSpace$transparentSpace${mostRecentWords[0]}$transparentSpace${mostRecentWords[1]}"))
        }
    }

    fun setStreamList(newListOfStreams: List<StreamResponse>) {
        val diffCallback = StreamItemDiffCallback(this.listOfStreams, newListOfStreams)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        diffResult.dispatchUpdatesTo(this)

        this.listOfStreams.clear()
        this.listOfStreams = newListOfStreams.map { it.copy() } as MutableList<StreamResponse>

        recyclerView.setMediaObjects(listOfStreams as ArrayList<StreamResponse>)

        if (isListInitial && listOfStreams.isNotEmpty()) {
            recyclerView.afterMeasured {
                this.playVideo()
            }
            isListInitial = false
        }

    }

    fun setStreamListForceUpdate(newListOfStreams: List<StreamResponse>) {
        this.listOfStreams.clear()
        this.listOfStreams = newListOfStreams.map { it.copy() } as MutableList<StreamResponse>

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

    private fun getHeightNeeded(isForPost: Boolean = false): Int {
        val displayMetrics = DisplayMetrics()
        context.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels
        screenWidth = width
        return if (isForPost) {
            width
        } else {
            width / 4 * 3
        }
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
            VIEW_POST -> return PostViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_post,
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
            is PostViewHolder -> holder.cleanup()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is LiveVideoViewHolder -> holder.bindView(listOfStreams[position])
            is VODViewHolder -> holder.bindView(listOfStreams[position])
            is ProgressHolder -> holder.bindView()
            is JumpToTopHolder -> holder.bindView(listOfStreams[position])
            is PostViewHolder -> holder.bindView(listOfStreams[position])
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
                            holder.setViews(bundle.getLong(key).toString())
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
            return when {
                listOfStreams[position].isLive -> {
                    VIEW_LIVE
                }
                listOfStreams[position].type != StreamResponseType.POST  -> {
                    VIEW_VOD
                }
                else -> {
                    VIEW_POST
                }
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
                .load(R.drawable.antourage_ic_incognito_user)
        picasso
            .placeholder(R.drawable.antourage_ic_incognito_user)
            .error(R.drawable.antourage_ic_incognito_user)
            .into(imageView)
    }

    inner class LiveVideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var parent: View = itemView

        fun setViews(views: String) {
            itemView.txtViewersCount_live.text = views.toLong().formatQuantity()
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

                    loadStreamerImageOrShowPlaceholder(creatorImageUrl, ivStreamerPicture_live)

//                    isChatEnabled?.let {
//                        if (!it && lastMessage.isNullOrEmpty()) {
//                            btnChat_live.gone(true)
//                        } else {
//                            btnChat_live.visibility = View.VISIBLE
//                        }
//                    }

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
                    txtCommentAuthor_live.text =
                        lastMessageAuthor?.let { formatAuthor(it, resources) }

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
                    txtViewersCount_live.text = viewersCount?.formatQuantity() ?: "0"
                    txtViewersCount_live.gone(viewersCount == null)
                    val formattedStartTime = context.resources.getString(R.string.ant_prefix_live_in_progress, startTime?.parseDateLong(context)?.toLowerCase())
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
        var parent: View = itemView

        fun setViews(views: String) {
            itemView.txtViewsCount_vod.text = views
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
                        if(images?.isNotEmpty() == true) images[0] else "",
                        ivThumbnail_vod,
                        ivThumbnail_vod_placeholder
                    )
                    loadStreamerImageOrShowPlaceholder(creatorImageUrl, ivStreamerPicture_vod)
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

                    if(isNew == null || !isNew!!){
                        txtNewBadge_vod.gone(true)
                    }else{
                        txtNewBadge_vod.gone(false)
                    }

                    txtTitle_vod.text = videoName
                    txtComment_vod.gone(lastMessage.isNullOrEmpty())
                    txtCommentAuthor_vod.gone(lastMessage.isNullOrEmpty())
//                    btnChat_vod.gone(lastMessage.isNullOrEmpty())
                    txtComment_vod.text = lastMessage
                    txtCommentAuthor_vod.text =
                        lastMessageAuthor?.let { formatAuthor(it, resources) }

                    val formattedStartTime = if(type == StreamResponseType.VOD){
                        duration?.parseToMills()?.plus((startTime?.parseToDate()?.time ?: 0))?.let {
                            Date(it).parseToDisplayAgoTimeLong(context, vod.type)
                        }
                    }else {
                        publishDate?.parseToDate()?.time?.let {
                            Date(it).parseToDisplayAgoTimeLong(context, vod.type)
                        }
                    }
                    val streamerNameAndTime = "$creatorNickname  •  $formattedStartTime"
                    txtStreamerInfo_vod.text = streamerNameAndTime
                    txtStreamerInfo_vod.visible(!formattedStartTime.isNullOrEmpty())
                    txtDuration_vod.text = duration?.parseToMills()?.millisToTime()
                    txtDuration_vod.gone(duration == null || duration.isEmpty())
                    txtViewsCount_vod.text = viewsCount?.formatQuantity() ?: "0"
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

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var parent: View = itemView

        fun bindView(post: StreamResponse) {
            parent.tag = this
            with(itemView) {
                post.apply {
                    if (images != null) {
                        initViewPager(imagesViewPager, dots, images, layoutPosition)
                    }
                    loadStreamerImageOrShowPlaceholder(creatorImageUrl, ivStreamerPicture_post)

                    if(images!=null && images.size > 1) {
                        shadowPost.visibility = View.VISIBLE
                    }else{
                        shadowPost.visibility = View.INVISIBLE
                    }
                    if(isNew == null || !isNew!!){
                        txtNewBadge_post.gone(true)
                    }else{
                        txtNewBadge_post.gone(false)
                    }

                    txtTitle_post.text = videoName
                    val formattedStartTime = publishDate?.parseToDate()?.parseToDisplayAgoTimeLong(
                        context,
                        post.type!!
                    )
                    val streamerNameAndTime = "$creatorNickname  •  $formattedStartTime"
                    txtStreamerInfo_post.text = streamerNameAndTime
                    txtStreamerInfo_post.visible(!formattedStartTime.isNullOrEmpty())
                }
            }
        }

        fun cleanup() {
            listOfStreams[layoutPosition].id?.let { viewPageStates.put(it, itemView.imagesViewPager.currentItem) }
        }

        private fun initViewPager(
            imagesViewPager: ViewPager2,
            dots: TabLayout,
            images: List<String>,
            layoutPosition: Int
        ) {
            val lp = imagesViewPager.layoutParams
            lp.height = Resources.getSystem().displayMetrics.widthPixels
            imagesViewPager.layoutParams = lp

            val adapter = ImageSwiperAdapter(images)
            imagesViewPager.adapter = adapter

            if (images.size > 1) {
                imagesViewPager.setCurrentItem(if (viewPageStates.containsKey(listOfStreams[layoutPosition].id)) viewPageStates[listOfStreams[layoutPosition].id]!! else 0, false)
                TabLayoutMediator(dots, imagesViewPager) { _, _ ->
                }.attach()
                dots.visibility = View.VISIBLE
            } else {
                dots.visibility = View.GONE
            }
        }
    }

    inner class ImageSwiperAdapter(
        private val list: List<String>
    ) : RecyclerView.Adapter<ImageSwiperAdapter.ImageSwiper>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageSwiper {
            val view: View = LayoutInflater.from(parent.context).inflate(
                R.layout.item_image_from_post,
                parent, false
            )
            return ImageSwiper(view)
        }

        override fun onBindViewHolder(holder: ImageSwiper, position: Int) {
                if (list[position].isNotBlank()) Picasso.get()
                    .load(list[position])
                    .placeholder(R.drawable.antourage_ic_photo_placeholder)
                    .fit()
                    .into(holder.imageView)
                else
                    Picasso.get()
                        .load(R.drawable.antourage_ic_photo_placeholder)
                        .into(holder.imageView)
        }

        override fun getItemCount(): Int {
            return list.size
        }

        inner class ImageSwiper(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById(R.id.imageContentView)
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