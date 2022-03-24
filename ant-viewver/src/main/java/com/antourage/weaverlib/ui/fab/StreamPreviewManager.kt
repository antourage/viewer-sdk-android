package com.antourage.weaverlib.ui.fab

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.*
import okhttp3.OkHttpClient
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource


@Keep
internal class StreamPreviewManager {

    companion object {
        private var callback: StreamCallback? = null
        private var player: ExoPlayer? = null
        private var streamUrl: String? = null

        fun setCallback(callback: StreamCallback) {
            StreamPreviewManager.callback = callback
        }

        fun startPlayingStream(streamUrl: String, context: Context, curtainMilliseconds: Long? = null): ExoPlayer? {
            player = buildExoPlayer(context)
            this.streamUrl = streamUrl
            player?.volume = 0f
            player?.setMediaSource(getMediaSource(streamUrl, context))
            player?.prepare()
            curtainMilliseconds?.let {
                player?.seekTo(it)
            }
            initEventListener()
            return player
        }

        private fun buildExoPlayer(context: Context): ExoPlayer {
            val adaptiveTrackSelection = AdaptiveTrackSelection.Factory()
            val trackSelector = DefaultTrackSelector(context, adaptiveTrackSelection)
            trackSelector.parameters = DefaultTrackSelector.ParametersBuilder(context).build()
            val loadControl = DefaultLoadControl.Builder()
                .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
                .setBufferDurationsMs(
                    2500,
                    5000,
                    2500,
                    2500
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
            return ExoPlayer.Builder(context).setLoadControl(loadControl)
                .setTrackSelector(trackSelector).build()
        }

        private fun getMediaSource(uri: String, context: Context): MediaSource {
            val okHttpClient = OkHttpClient.Builder()
                .addNetworkInterceptor { chain ->
                    val request =
                        chain.request().newBuilder().addHeader("Connection", "close").build()
                    chain.proceed(request)
                }
                .build()

            return if (uri.endsWith("mp4", true) || uri.endsWith("flv", true) || uri.endsWith(
                    "mov",
                    true
                )
            ) {
                val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
                ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(
                    MediaItem.fromUri(
                        uri
                    )
                )
            } else {
                val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
                HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(uri))
            }
        }


        private fun initEventListener() {
            player?.addListener(playerEventListener)
        }

        private val playerEventListener = object : Player.Listener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                callback?.onNewState(playbackState)
            }

            override fun onPlayerError(error: PlaybackException) {
                callback?.onError()
                Log.e(AntourageFab.TAG, "player error: ${error.cause.toString()}")
            }
        }

        fun releasePlayer() {
            player?.release()
            player?.removeListener(playerEventListener)
        }

        fun removeEventListener() {
            player?.removeListener(playerEventListener)
        }
    }

    @Keep
    internal interface StreamCallback {
        fun onNewState(playbackState: Int) {}
        fun onError() {}
    }
}