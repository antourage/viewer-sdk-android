package com.antourage.weaverlib.screens.list

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.pm.ActivityInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.graphics.drawable.Animatable2Compat
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.di.injector
import com.antourage.weaverlib.other.dp2px
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.replaceFragment
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog
import com.antourage.weaverlib.screens.list.rv.VerticalSpaceItemDecorator
import com.antourage.weaverlib.screens.list.rv.VideosAdapter
import com.antourage.weaverlib.screens.list.rv.VideosLayoutManager
import com.antourage.weaverlib.screens.vod.VodPlayerFragment
import com.antourage.weaverlib.screens.weaver.PlayerFragment
import kotlinx.android.synthetic.main.fragment_videos_list.*

class VideoListFragment : Fragment() {

    private lateinit var videoAdapter: VideosAdapter
    private lateinit var viewModel: VideoListViewModel
    private var loader: AnimatedVectorDrawableCompat? = null

    companion object {
        fun newInstance(): VideoListFragment {
            return VideoListFragment()
        }
    }

    //region Observers
    private val streamsObserver: Observer<List<StreamResponse>> = Observer { list ->
        list?.let { videoAdapter.setStreamList(it) }
        videoRefreshLayout.isRefreshing = false
    }

    private val loaderObserver: Observer<Boolean> = Observer { show ->
        if (show == true) {
            showLoading()
        } else {
            hideLoading()
        }
    }

    private val beChoiceObserver: Observer<Boolean> = Observer {
        if (it != null && it)
            context?.let { context -> DevSettingsDialog(context, viewModel).show() }
    }

    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, activity?.injector?.getVideoListViewModelFactory())
            .get(VideoListViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_videos_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
        subscribeToObservers()
    }

    private fun subscribeToObservers() {
        viewModel.listOfStreams.observe(this.viewLifecycleOwner, streamsObserver)
        viewModel.loaderLiveData.observe(this.viewLifecycleOwner, loaderObserver)
        viewModel.getShowBeDialog().observe(this.viewLifecycleOwner, beChoiceObserver)
    }

    override fun onResume() {
        super.onResume()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        viewModel.subscribeToLiveStreams()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    private fun initUi() {
        val onClick: (stream: StreamResponse) -> Unit = { streamResponse ->
            if (streamResponse.isLive) {
                replaceFragment(PlayerFragment.newInstance(streamResponse), R.id.mainContent, true)
            } else {
                context?.let { context ->
                    streamResponse.streamId?.let {
                        UserCache.newInstance().saveVideoToSeen(context, it)
                    }
                }
                replaceFragment(
                    VodPlayerFragment.newInstance(streamResponse),
                    R.id.mainContent,
                    true
                )
            }
        }
        videoAdapter = VideosAdapter(onClick)
        videosRV.adapter = videoAdapter
        videosRV.layoutManager = VideosLayoutManager(context)
        val dividerItemDecoration = VerticalSpaceItemDecorator(
            dp2px(context!!, 30f).toInt()
        )
        videosRV.addItemDecoration(dividerItemDecoration)
        videoRefreshLayout.setOnRefreshListener {
            viewModel.refreshVODs()
        }
        ivClose.setOnClickListener { activity?.finish() }
        viewBEChoice.setOnClickListener { viewModel.onLogoPressed() }
        initLoader()
    }

    private fun initLoader() {
        loader = context?.let { AnimatedVectorDrawableCompat.create(it, R.drawable.loader_logo) }
        ivLoader.setImageDrawable(loader)
    }

    private fun showLoading() {
        loader?.apply {
            if (!isRunning) {
                registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                    override fun onAnimationEnd(drawable: Drawable?) {
                        ivLoader.post { start() }
                    }
                })
                start()
                ivLoader.visibility = View.VISIBLE
            }
        }
    }

    private fun hideLoading() {
        loader?.apply {
            if (ivLoader.visibility == View.VISIBLE && isRunning) {
                ivLoader.visibility = View.INVISIBLE
                clearAnimationCallbacks()
                stop()
            }
        }
    }
}
