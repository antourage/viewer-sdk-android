package com.antourage.weaverlib.screens.list.rv

import android.content.Context
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.convertUtcToLocal
import com.antourage.weaverlib.other.hideWithAnimation
import com.antourage.weaverlib.other.millisToTime
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.models.Video
import com.antourage.weaverlib.other.revealWithAnimation
import com.antourage.weaverlib.other.room.RoomRepository
import com.antourage.weaverlib.screens.base.Repository
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoListener
import kotlinx.android.synthetic.main.item_live_video.view.*
import kotlinx.android.synthetic.main.item_vod.view.*
import okhttp3.OkHttpClient
import java.util.*

private const val AUTO_PLAY_DEBOUNCE: Long = 500

internal class VideoPlayerRecyclerView : RecyclerView {

    // ui
    private var thumbnail: ImageView? = null
    private var duration: TextView? = null
    private var autoPlayContainer: ConstraintLayout? = null
    private var autoPlayImageView: ImageView? = null
    private var autoPlayTextView: TextView? = null
    private var viewHolderParent: View? = null
    private var frameLayout: FrameLayout? = null
    private var videoSurfaceView: PlayerView? = null
    private var videoPlayer: SimpleExoPlayer? = null
    private var replayView: ConstraintLayout? = null
    private var watchingProgress: ProgressBar? = null
    private var autoPlayAnimatedDrawable: AnimatedVectorDrawableCompat? = null

    private lateinit var roomRepository: RoomRepository

    // vars
    private var streams: ArrayList<StreamResponse> = ArrayList()
    private var videoSurfaceDefaultHeight = 0
    private var screenDefaultHeight = 0
    var playPosition = -1
    private var isVideoViewAdded = false
    private var playHandler = Handler(Looper.getMainLooper())
    private var durationHandler = Handler(Looper.getMainLooper())
    private var shouldSetStopTime: Boolean = false

    var isOpeningPlayer: Boolean = false


    private var currentlyPlayingVideo: StreamResponse? = null
    var fullyViewedVods = mutableSetOf<StreamResponse>()

    constructor(@NonNull context: Context) : super(context) {
        init()
    }

    constructor(
        @NonNull context: Context,
        @Nullable attrs: AttributeSet?
    ) : super(context, attrs) {
        init()
    }

    fun setRoomRepository(repo: RoomRepository) {
        roomRepository = repo
    }

    fun resumePlaying() {
        if (streams.isNotEmpty()) {
            playHandler.removeCallbacksAndMessages(null)
            playHandler.postDelayed({
                if (!videoPlayer?.isPlaying!!) {
                    playPosition = -1
                    playVideo()
                }
            }, AUTO_PLAY_DEBOUNCE)
        }
    }

    fun onResume() {
        initPlayer()
    }

    fun onRefresh() {
//        releasePlayer()
//        onPause()
//        onResume()
    }

    private fun initPlayer() {
        videoSurfaceView = PlayerView(this.context.applicationContext)
        videoSurfaceView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        val adaptiveTrackSelection = AdaptiveTrackSelection.Factory()
        val trackSelector: TrackSelector = DefaultTrackSelector(adaptiveTrackSelection)

        videoPlayer = ExoPlayerFactory.newSimpleInstance(context.applicationContext, trackSelector)
        videoSurfaceView?.useController = false
        videoSurfaceView?.player = videoPlayer
        videoPlayer?.volume = 0f
        videoPlayer?.addListener(object : Player.EventListener {
            override fun onTimelineChanged(
                timeline: Timeline,
                @Nullable manifest: Any?,
                reason: Int
            ) {
            }

            override fun onTracksChanged(
                trackGroups: TrackGroupArray,
                trackSelections: TrackSelectionArray
            ) {
            }

            override fun onLoadingChanged(isLoading: Boolean) {}
            override fun onPlayerStateChanged(
                playWhenReady: Boolean,
                playbackState: Int
            ) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                    }
                    Player.STATE_ENDED -> {
                        showReplayLayout()
                    }
                    Player.STATE_IDLE -> {
                    }
                    Player.STATE_READY -> {
                        if (!isVideoViewAdded) {
                            addVideoView()
                        }
                    }
                    else -> {
                    }
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {}
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}
            override fun onPlayerError(error: ExoPlaybackException) {
                hideAutoPlayLayout()
            }

            override fun onPositionDiscontinuity(reason: Int) {}
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {}
            override fun onSeekProcessed() {}
        })

        videoPlayer?.addVideoListener(object : VideoListener {
            override fun onRenderedFirstFrame() {
                thumbnail?.hideWithAnimation()
            }
        })

    }

    private fun init() {
        val display =
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val point = Point()
        display.getSize(point)
        videoSurfaceDefaultHeight = point.x
        screenDefaultHeight = point.y
        initPlayer()
        addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                playHandler.removeCallbacksAndMessages(null)
                if (newState == SCROLL_STATE_IDLE) {
                    val position = getTargetPosition()
                    if (position != playPosition && !fullyViewedVods.contains(currentlyPlayingVideo)) {
                        hideAutoPlayLayout()
                    }
                    playHandler.postDelayed({
                        playVideo()
                    }, AUTO_PLAY_DEBOUNCE)
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                playHandler.removeCallbacksAndMessages(null)
            }
        })
        addOnChildAttachStateChangeListener(object : OnChildAttachStateChangeListener {
            override fun onChildViewDetachedFromWindow(view: View) {
                if (viewHolderParent != null && viewHolderParent == view) {
                    resetVideoView()
                }
            }

            override fun onChildViewAttachedToWindow(view: View) {
            }
        })

        initAutoPlayAnimation()
    }

    fun forceRestartAutoPlayOnChange() {
        hideAutoPlayLayout()
        playHandler.postDelayed({
            playVideo()
        }, AUTO_PLAY_DEBOUNCE)
    }

    //todo: test whether correctly parsed
    private fun getExpirationDate(vodId: Int): Long {
        return Repository.vods?.find { video -> video.id?.equals(vodId) ?: false }
            ?.startTime?.let { convertUtcToLocal(it)?.time } ?: 0L
    }

    /**
     * set stopWatching time to avoid additional call to DB when
     * turning back from player to videos list screen
     */
    private fun setVODStopWatchingTimeLocally(vodId: Int, stopWatchingTime: Long) {
        Repository.vods?.find { streamResponse -> streamResponse.id?.equals(vodId) ?: false }
            ?.stopTimeMillis = stopWatchingTime
    }

    private fun setVodStopWatchingTime() {
        val vodId = currentlyPlayingVideo?.id
        vodId?.let {
            val stopWatchingTime = videoPlayer?.currentPosition ?: 0
            val duration = videoPlayer?.duration ?: 0L

            if (stopWatchingTime > 0) {
                if (duration != 0L && duration * 0.9 - stopWatchingTime < 0) {
                    roomRepository.addStopTime(Video(vodId, 0, getExpirationDate(vodId)))
                } else {
                    roomRepository.addStopTime(
                        Video(vodId, stopWatchingTime, getExpirationDate(vodId))
                    )
                }
                setVODStopWatchingTimeLocally(vodId, stopWatchingTime)
            }
        }
    }

    fun hideAutoPlayLayout() {
        if (shouldSetStopTime) {
            setVodStopWatchingTime()
            shouldSetStopTime = false
        }

        if (isVideoViewAdded && thumbnail?.alpha != 1f) {
            thumbnail?.revealWithAnimation()
            autoPlayContainer?.hideWithAnimation()
            if (autoPlayContainer?.id == R.id.autoPlayContainer_vod) {
                duration?.revealWithAnimation()
            }
        }
        stopDurationUpdate()
        videoPlayer?.stop()
        playPosition = -1
    }

    private fun showReplayLayout() {
        if (autoPlayContainer?.id == R.id.autoPlayContainer_vod) {
            currentlyPlayingVideo?.let { fullyViewedVods.add(it) }
            replayView?.revealWithAnimation()
            duration?.revealWithAnimation()
            thumbnail?.revealWithAnimation()
            autoPlayContainer?.hideWithAnimation()
            watchingProgress?.hideWithAnimation()
            if (shouldSetStopTime) {
                setVodStopWatchingTime()
                shouldSetStopTime = false
            }
            stopDurationUpdate()
        }
    }

    fun onPause() {
        releasePlayer()
        if (shouldSetStopTime) {
            setVodStopWatchingTime()
            shouldSetStopTime = false
        }
        playHandler.removeCallbacksAndMessages(null)
        stopDurationUpdate()
    }

    private fun getTargetPosition(): Int {
        val targetPosition: Int
        val startPosition: Int =
            (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        var endPosition: Int =
            (layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

        // if there is more than 2 list-items on the screen, set the difference to be 1
        if (endPosition - startPosition > 1) {
            endPosition = startPosition + 1
        }

        // something is wrong. return.
        if (startPosition < 0 || endPosition < 0) {
            return -1
        }

        // if there is more than 1 list-item on the screen
        targetPosition = if (startPosition != endPosition) {
            val startPositionVideoHeight = getVisibleVideoSurfaceHeight(startPosition)
            val endPositionVideoHeight = getVisibleVideoSurfaceHeight(endPosition)
            if (startPositionVideoHeight > endPositionVideoHeight) startPosition else endPosition
        } else {
            startPosition
        }

        return targetPosition
    }

    fun playVideo() {
        if (!videoPlayer?.isPlaying!!) {
            shouldSetStopTime = true
            val targetPosition: Int = getTargetPosition()
            // video is already playing so return
            if (targetPosition == playPosition || targetPosition == -1) return

            playPosition = targetPosition
            videoSurfaceView?.visibility = INVISIBLE
            removeVideoView(videoSurfaceView)

            if (fullyViewedVods.isNotEmpty() && fullyViewedVods.contains(streams[targetPosition])) return

            val currentPosition: Int =
                targetPosition - (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            val child: View = getChildAt(currentPosition) ?: return
            var mediaUrl: String? = null
            var stopTime: Long? = null
            var holder: ViewHolder? = null

            if (child.tag is VideosAdapter.LiveVideoViewHolder) {
                holder = child.tag as VideosAdapter.LiveVideoViewHolder
            } else if (child.tag is VideosAdapter.VODViewHolder) {
                holder = child.tag as VideosAdapter.VODViewHolder
            }

            if (holder == null) {
                playPosition = -1
                return
            }

            if (child.tag is VideosAdapter.LiveVideoViewHolder) {
                frameLayout = holder.itemView.mediaContainer_live
                thumbnail = holder.itemView.ivThumbnail_live
                mediaUrl = streams[targetPosition].hlsUrl?.get(0)
                currentlyPlayingVideo = streams[targetPosition]
                autoPlayContainer = holder.itemView.autoPlayContainer_live
                autoPlayImageView = holder.itemView.ivAutoPlay_live
                autoPlayTextView = holder.itemView.txtAutoPlayDuration_live
            } else if (child.tag is VideosAdapter.VODViewHolder) {
                frameLayout = holder.itemView.mediaContainer_vod
                thumbnail = holder.itemView.ivThumbnail_vod
                mediaUrl = streams[targetPosition].videoURL
                stopTime = streams[targetPosition].stopTimeMillis
                currentlyPlayingVideo = streams[targetPosition]
                autoPlayContainer = holder.itemView.autoPlayContainer_vod
                autoPlayImageView = holder.itemView.ivAutoPlay_vod
                autoPlayTextView = holder.itemView.txtAutoPlayDuration_vod
                duration = holder.itemView.txtDuration_vod
                replayView = holder.itemView.replayContainer
                watchingProgress = holder.itemView.watchingProgress
            }

            viewHolderParent = holder.itemView
            videoSurfaceView?.player = videoPlayer

            val videoSource: MediaSource
            if (mediaUrl != null) {
                videoSource = getMediaSource(mediaUrl, context)
                videoPlayer?.prepare(videoSource)
                stopTime?.let { videoPlayer?.seekTo(it) }
                videoPlayer?.playWhenReady = true
            }
        }
    }

    private fun getMediaSource(uri: String, context: Context): MediaSource {
        val okHttpClient = OkHttpClient.Builder()
            .addNetworkInterceptor { chain ->
                val request = chain.request().newBuilder().addHeader("Connection", "close").build()
                chain.proceed(request)
            }
            .build()

        return if(uri.endsWith("mp4", true) || uri.endsWith("flv", true) ||  uri.endsWith("mov", true)){
            val dataSourceFactory: DataSource.Factory = DefaultHttpDataSourceFactory()
            ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(
                MediaItem.fromUri(
                    uri
                )
            )
        }else {
            val okHttpDataSourceFactory =
                OkHttpDataSourceFactory(okHttpClient, Util.getUserAgent(context, "Exo2"))
            HlsMediaSource.Factory(okHttpDataSourceFactory).createMediaSource(MediaItem.fromUri(uri))
        }
    }


    /**
     * Returns the visible region of the video surface on the screen.
     * if some is cut off, it will return less than the @videoSurfaceDefaultHeight
     * @param playPosition
     * @return
     */
    private fun getVisibleVideoSurfaceHeight(playPosition: Int): Int {
        val at: Int =
            playPosition - (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        val child: View = getChildAt(at) ?: return 0
        val location = IntArray(2)
        child.getLocationInWindow(location)
        return if (location[1] < 0) {
            location[1] + videoSurfaceDefaultHeight
        } else {
            screenDefaultHeight - location[1]
        }
    }

    // Remove the old player
    private fun removeVideoView(videoView: PlayerView?) {
        if (videoView == null) return
        if (videoView.parent == null) return

        val parent = (videoView.parent as ViewGroup)
        val index = parent.indexOfChild(videoView)
        if (index >= 0) {
            parent.removeViewAt(index)
            isVideoViewAdded = false
        }
    }

    private fun addVideoView() {
        frameLayout?.addView(videoSurfaceView)
        isVideoViewAdded = true
        videoSurfaceView?.requestFocus()
        videoSurfaceView?.visibility = VISIBLE
        videoSurfaceView?.alpha = 1f
        autoPlayContainer?.revealWithAnimation()
        animateAutoPlay()
        startDurationUpdate()
        if (autoPlayContainer?.id == R.id.autoPlayContainer_vod) {
            if (watchingProgress?.visibility == View.INVISIBLE) watchingProgress?.revealWithAnimation()
            duration?.hideWithAnimation()
        }
    }

    private fun initAutoPlayAnimation() {
        autoPlayAnimatedDrawable = context?.let {
            AnimatedVectorDrawableCompat.create(
                it,
                R.drawable.antourage_autoplay_animation
            )
        }
    }

    private fun animateAutoPlay() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            initAutoPlayAnimation()
        }

        autoPlayImageView?.setImageDrawable(null)
        autoPlayImageView?.setImageDrawable(autoPlayAnimatedDrawable)
        autoPlayAnimatedDrawable?.apply {
            clearAnimationCallbacks()
            registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    animateAutoPlay()
                }
            })
            start()
        }
    }

    private fun startDurationUpdate() {
        durationHandler.postDelayed(object : Runnable {
            override fun run() {
                //vod
                if (autoPlayContainer?.id == R.id.autoPlayContainer_vod) {
                    autoPlayTextView?.text = (videoPlayer?.currentPosition?.let {
                        videoPlayer?.duration?.minus(
                            it
                        )
                    })?.millisToTime()
                    val dur = videoPlayer?.duration?.toInt() ?: 1
                    val current = videoPlayer?.currentPosition?.toInt() ?: 0
                    watchingProgress?.progress = (current * 100) / (dur)
                } else {
                    //live
                    val liveDuration =
                        ((System.currentTimeMillis() - (currentlyPlayingVideo?.startTime?.let {
                            convertUtcToLocal(it)?.time
                        } ?: 0))).millisToTime()

                    autoPlayTextView?.text = liveDuration
                }
                durationHandler.postDelayed(this, 1000)
            }
        }, 0)
    }

    private fun stopDurationUpdate() {
        durationHandler.removeCallbacksAndMessages(null)
    }

    fun resetVideoView() {
        if (isVideoViewAdded) {
            removeVideoView(videoSurfaceView)
            playPosition = -1
            videoSurfaceView?.visibility = INVISIBLE
            videoSurfaceView?.alpha = 1f
            thumbnail?.visibility = VISIBLE
            thumbnail?.alpha = 1f
            autoPlayContainer?.visibility = View.INVISIBLE
            autoPlayContainer?.alpha = 1f
            autoPlayImageView?.clearAnimation()
            autoPlayAnimatedDrawable?.clearAnimationCallbacks()
            autoPlayAnimatedDrawable?.stop()
            stopDurationUpdate()
            if (autoPlayContainer?.id == R.id.autoPlayContainer_vod) {
                duration?.visibility = View.VISIBLE
                duration?.alpha = 1f
            }
        }
    }

    private fun releasePlayer() {
        videoPlayer?.release()
        viewHolderParent = null
    }

    fun setMediaObjects(mediaObjects: ArrayList<StreamResponse>) {
        this.streams = mediaObjects
    }

    fun resetPlayPosition() {
        playPosition = -1
    }
}