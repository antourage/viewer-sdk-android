package com.antourage.weaverlib.ui.fab

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.Keep
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.util.Util
import okhttp3.OkHttpClient

@Keep
internal class StreamPreviewManager {

    companion object {
        private var callback: StreamCallback? = null
        private var player: SimpleExoPlayer? = null
        private var streamUrl: String? = null

        fun setStreamManager(callback: StreamCallback) {
            StreamPreviewManager.callback = callback
        }

        fun getExoPlayer(streamUrl: String, context: Context): SimpleExoPlayer? {
            player = getSimpleExoPlayer(context)
            this.streamUrl = streamUrl
            player?.playWhenReady = true
            player?.volume = 0f
            player?.setMediaSource(getMediaSource(streamUrl, context))
            player?.prepare()
            initEventListener()
            return player
        }

        private fun getSimpleExoPlayer(context: Context): SimpleExoPlayer {
            val adaptiveTrackSelection = AdaptiveTrackSelection.Factory()
            val trackSelector = DefaultTrackSelector(adaptiveTrackSelection)
            trackSelector.parameters = DefaultTrackSelector.ParametersBuilder().build()
            val loadControl = DefaultLoadControl.Builder()
                .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
                .setBufferDurationsMs(
                    2500,
                    5000,
                    2500,
                    2500
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .createDefaultLoadControl()
            return ExoPlayerFactory.newSimpleInstance(
                context,
                DefaultRenderersFactory(context),
                trackSelector,
                loadControl
            )
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


        private fun initEventListener() {
            player?.addListener(playerEventListener)
        }

        private val playerEventListener = object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                callback?.onNewState(playbackState)
            }
            override fun onPlayerError(err: ExoPlaybackException) {
                callback?.onError()
                Log.d(AntourageFab.TAG, "player error: ${err.cause.toString()}")
                Log.d(
                    AntourageFab.TAG, when (err.type) {
                        ExoPlaybackException.TYPE_SOURCE -> "error type: ${err.type} error message: ${err.sourceException.message}"
                        ExoPlaybackException.TYPE_RENDERER -> "error type: ${err.type} error message: ${err.rendererException.message}"
                        ExoPlaybackException.TYPE_UNEXPECTED -> "error type: ${err.type} error message: ${err.unexpectedException.message}"
                        ExoPlaybackException.TYPE_REMOTE -> "error type: ${err.type} error message: ${err.message}"
                        ExoPlaybackException.TYPE_OUT_OF_MEMORY -> "error type: ${err.type} error message: ${err.outOfMemoryError.message}"
                        else -> err.message
                    } ?: ""
                )

            }
        }

        fun releasePlayer(){
            player?.release()
            player?.removeListener(playerEventListener)
        }

        fun removeEventListener(){
            player?.removeListener(playerEventListener)
        }
    }

    @Keep
    internal interface StreamCallback {
        fun onNewState(playbackState: Int){}
        fun onError(){}
    }
}