package com.antourage.weaverlib.screens.list

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.di.injector
import com.antourage.weaverlib.other.dp2px
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.ConnectionStateMonitor
import com.antourage.weaverlib.other.networking.NetworkConnectionState
import com.antourage.weaverlib.other.replaceFragment
import com.antourage.weaverlib.screens.base.BaseFragment
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog
import com.antourage.weaverlib.screens.list.rv.VerticalSpaceItemDecorator
import com.antourage.weaverlib.screens.list.rv.VideosAdapter
import com.antourage.weaverlib.screens.list.rv.VideosLayoutManager
import com.antourage.weaverlib.screens.vod.VodPlayerFragment
import com.antourage.weaverlib.screens.weaver.PlayerFragment
import kotlinx.android.synthetic.main.fragment_videos_list.*

internal class VideoListFragment : BaseFragment<VideoListViewModel>(),
    MyNestedScrollView.OnBottomReachedListener {

    override fun getLayoutId() = R.layout.fragment_videos_list

    private lateinit var videoAdapter: VideosAdapter
    private lateinit var placeHolderAdapter: VideoPlaceholdersAdapter
    private lateinit var rvLayoutManager: VideosLayoutManager
    private val loadingAnimHandler = Handler()

    companion object {
        fun newInstance(): VideoListFragment {
            return VideoListFragment()
        }
    }

    private val loadingAnimRunnable = object : Runnable {
        override fun run() {
            placeHolderAdapter.shiftItems()
            loadingAnimHandler.postDelayed(this, 350)
        }
    }

    //region Observers
    private val streamsObserver: Observer<List<StreamResponse>> = Observer { list ->
        list?.let {
            if (list.isNullOrEmpty()) {
                showEmptyListPlaceholder()
            } else {
                videoAdapter.setStreamList(it)
                nestedSV.setBottomReachesListener(this@VideoListFragment)
                hidePlaceholder()
            }
        }
        videoRefreshLayout.isRefreshing = false
    }

    private val loaderObserver: Observer<Boolean> = Observer { show ->
        if (show == true) {
            showLoadingLayout()
        } else {
            hideLoadingLayout()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToObservers()
    }

    private fun subscribeToObservers() {
        viewModel.listOfStreams.observe(this.viewLifecycleOwner, streamsObserver)
        viewModel.loaderLiveData.observe(this.viewLifecycleOwner, loaderObserver)
        viewModel.getShowBeDialog().observe(this.viewLifecycleOwner, beChoiceObserver)
        ConnectionStateMonitor.internetStateLiveData.observe(
            this.viewLifecycleOwner,
            networkStateObserver
        )
    }

    override fun onResume() {
        super.onResume()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        context?.let {
            if (ConnectionStateMonitor.isNetworkAvailable(it)) {
                viewModel.subscribeToLiveStreams()
                viewModel.refreshVODsLocally()
            } else {
                showEmptyListPlaceholder()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
        loadingAnimHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        videosRV.adapter = null
    }

    override fun onBottomReached(view: View?) {
        val total = rvLayoutManager.itemCount
        val lastVisibleItem = rvLayoutManager.findLastCompletelyVisibleItemPosition()
        if (total <= lastVisibleItem + 1 && videoAdapter.getStreams()[lastVisibleItem].id == -2) {
            viewModel.refreshVODs(noLoadingPlaceholder = true)
            Log.d("REFRESH_VODS", "onBottomReached")
        }
    }

    override fun initUi(view: View?) {
        val onClick: (stream: StreamResponse) -> Unit = { streamResponse ->
            if (streamResponse.isLive) {
                replaceFragment(PlayerFragment.newInstance(streamResponse), R.id.mainContent, true)
            } else {
                context?.let { context ->
                    streamResponse.streamId?.let {
                        UserCache.getInstance(context)?.saveVideoToSeen(it)
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
        placeHolderAdapter = VideoPlaceholdersAdapter()

        initRecyclerView(videoAdapter, videosRV)
        initRecyclerView(placeHolderAdapter, placeHolderRV)

        rvLayoutManager = VideosLayoutManager(context)
        rvLayoutManager.reverseLayout = false
        videosRV.layoutManager = rvLayoutManager
        videoRefreshLayout.setOnRefreshListener {
            viewModel.refreshVODs(0, true)
        }

        placeHolderRV.layoutManager = LinearLayoutManager(context)

        ivClose.setOnClickListener { activity?.finish() }
        viewBEChoice.setOnClickListener { viewModel.onLogoPressed() }

        ReceivingVideosManager.setReceivingVideoCallback(viewModel)
        viewModel.refreshVODs()
    }

    private fun initRecyclerView(
        adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>,
        recyclerView: RecyclerView
    ) {
        recyclerView.adapter = adapter
        recyclerView.isNestedScrollingEnabled = false
        nestedSV.isNestedScrollingEnabled = true
        placeHolderNestedSV.isNestedScrollingEnabled = true
        val dividerItemDecoration = VerticalSpaceItemDecorator(
            dp2px(context!!, 30f).toInt()
        )
        recyclerView.addItemDecoration(dividerItemDecoration)
    }

    private fun hideLoadingLayout() {
        loadingAnimHandler.removeCallbacksAndMessages(null)
    }

    private fun showLoadingLayout() {
        showLoadingListPlaceholder()
        placeHolderAdapter.setItems(
            arrayListOf(
                R.color.ant_no_content_placeholder_color_3,
                R.color.ant_no_content_placeholder_color_2,
                R.color.ant_no_content_placeholder_color_1
            )
        )
        loadingAnimHandler.postDelayed(loadingAnimRunnable, 350)
    }

    private fun showEmptyListPlaceholder() {
        showLoadingListPlaceholder()
        placeHolderAdapter.setItems(
            arrayListOf(
                R.color.ant_no_content_placeholder_color_1,
                R.color.ant_no_content_placeholder_color_1,
                R.color.ant_no_content_placeholder_color_1
            )
        )
        tvTitle.visibility = View.INVISIBLE
        tvNoContent.visibility = View.VISIBLE

    }

    private fun showLoadingListPlaceholder() {
        nestedSV.visibility = View.INVISIBLE
        placeHolderNestedSV.visibility = View.VISIBLE
    }

    private fun hidePlaceholder() {
        tvTitle.visibility = View.VISIBLE
        tvNoContent.visibility = View.INVISIBLE

        placeHolderNestedSV.animate()
            .alpha(0.0f)
            .setDuration(600)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    placeHolderNestedSV?.visibility = View.INVISIBLE
                }
            })

        nestedSV.visibility = View.VISIBLE
    }

    private val networkStateObserver: Observer<NetworkConnectionState> = Observer { networkState ->
        when (networkState?.ordinal) {
            NetworkConnectionState.AVAILABLE.ordinal -> {
                viewModel.onNetworkGained()
            }
        }
    }
}
