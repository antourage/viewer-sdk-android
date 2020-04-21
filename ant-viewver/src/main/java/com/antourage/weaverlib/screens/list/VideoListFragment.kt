package com.antourage.weaverlib.screens.list

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.antourage.weaverlib.Global
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.di.injector
import com.antourage.weaverlib.other.betterSmoothScrollToPosition
import com.antourage.weaverlib.other.dp2px
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.ConnectionStateMonitor
import com.antourage.weaverlib.other.networking.NetworkConnectionState
import com.antourage.weaverlib.other.replaceFragment
import com.antourage.weaverlib.screens.base.BaseFragment
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog
import com.antourage.weaverlib.screens.list.rv.VerticalSpaceItemDecorator
import com.antourage.weaverlib.screens.list.rv.VideosAdapter2
import com.antourage.weaverlib.screens.vod.VodPlayerFragment
import com.antourage.weaverlib.screens.weaver.PlayerFragment
import kotlinx.android.synthetic.main.fragment_videos_list3.*
import kotlinx.android.synthetic.main.fragment_videos_list3.ivClose
import kotlinx.android.synthetic.main.fragment_videos_list3.placeHolderRV
import kotlinx.android.synthetic.main.fragment_videos_list3.tvNoContent
import kotlinx.android.synthetic.main.fragment_videos_list3.tvTitle
import kotlinx.android.synthetic.main.fragment_videos_list3.videoRefreshLayout
import kotlinx.android.synthetic.main.fragment_videos_list3.videosRV
import kotlinx.android.synthetic.main.fragment_videos_list3.viewBEChoice

internal class VideoListFragment : BaseFragment<VideoListViewModel>() {

    override fun getLayoutId() = R.layout.fragment_videos_list3

    private lateinit var videoAdapter: VideosAdapter2
    private lateinit var placeHolderAdapter: VideoPlaceholdersAdapter
    private lateinit var rvLayoutManager: PreCachingLayoutManager
    private val loadingAnimHandler = Handler()
    private var refreshVODs = true
    private var isLoading = false
    private var isNewLiveButtonShown = false
    private var isInitialListSet = true
    private var newLivesList = mutableListOf<StreamResponse>()

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
        list?.let { newStreams ->
            if (newStreams.isNullOrEmpty()) {
                showEmptyListPlaceholder()
            } else {
                isLoading = false
                checkIsNewLiveAdded(newStreams)
                checkIsLiveWasRemoved(newStreams)
                if(videoRefreshLayout.isRefreshing){
                    videoAdapter.setStreamListForceUpdate(newStreams)
                }else{
                    videoAdapter.setStreamList(newStreams)
                }
                isInitialListSet = false
                hidePlaceholder()
            }
        }
        videoRefreshLayout.isRefreshing = false
    }

    private fun checkIsLiveWasRemoved(newStreams: List<StreamResponse>){
        val iterator = newLivesList.iterator()
        for (stream in iterator) {
            if(newStreams.none() {it.id == stream.id}){
                iterator.remove()
            }
        }
        if(newLivesList.isEmpty()){
            triggerNewLiveButton(false)
        }
    }

    private fun checkIsNewLiveAdded(newStreams: List<StreamResponse>) {
        if (!isInitialListSet) {
            for (stream in newStreams) {
                if (stream.isLive && videoAdapter.getStreams().none { it.id == stream.id }) {
                    newLivesList.add(stream)
                    triggerNewLiveButton(true)
                    break
                }
            }
        }
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
        activity?.injector?.getVideoListViewModelFactory()?.let {
            viewModel = ViewModelProvider(this, it).get(VideoListViewModel::class.java)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToObservers()

        videosRV.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (isNewLiveButtonShown) {
                    if (rvLayoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
                        triggerNewLiveButton(false)
                    }
                }
            }
        })
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

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onResume() {
        super.onResume()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        context?.let {
            //                        viewModel.subscribeToLiveStreams()

            viewModel.handleUserAuthorization()

            viewModel.refreshVODsLocally()
            if (!ConnectionStateMonitor.isNetworkAvailable(it) &&
                viewModel.listOfStreams.value.isNullOrEmpty()
            ) {
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

    override fun initUi(view: View?) {
        val onClick: (stream: StreamResponse) -> Unit = { streamResponse ->
            when {
                streamResponse.isLive -> {
                    val userId = context?.let { UserCache.getInstance(it)?.getUserId() } ?: -1
                    replaceFragment(
                        PlayerFragment.newInstance(streamResponse, userId),
                        R.id.mainContent,
                        true
                    )
                }
                streamResponse.id == -1 -> {
                    videosRV.betterSmoothScrollToPosition(0)
                }
                else -> {
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
        }

        btnNewLive.setOnClickListener { videosRV.betterSmoothScrollToPosition(0) }

        videoAdapter = VideosAdapter2(onClick)
        placeHolderAdapter = VideoPlaceholdersAdapter()

        initRecyclerView(videoAdapter, videosRV)
        initRecyclerView(placeHolderAdapter, placeHolderRV)

        rvLayoutManager = PreCachingLayoutManager(context)
        rvLayoutManager.orientation = LinearLayoutManager.VERTICAL
        rvLayoutManager.setExtraLayoutSpace()

        videosRV.layoutManager = rvLayoutManager

        videosRV.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val total = rvLayoutManager.itemCount
                    val lastVisibleItem = rvLayoutManager.findLastCompletelyVisibleItemPosition()
                    if (!isLoading && total <= lastVisibleItem + 1 && videoAdapter.getStreams()[lastVisibleItem].id == -2) {
                        viewModel.refreshVODs(noLoadingPlaceholder = true)
                        isLoading = true
                        Log.d("REFRESH_VODS", "onBottomReached")
                    }
                }
            }
        })

        videoRefreshLayout.setOnRefreshListener {
            context?.let {
                if (ConnectionStateMonitor.isNetworkAvailable(it)) {
                    if (viewModel.userAuthorized()) {
                        viewModel.refreshVODs(0, true)
                    } else {
                        videoRefreshLayout.isRefreshing = false
                        showWarningAlerter(resources.getString(R.string.invalid_toke_error_message))
                    }
                } else {
                    videoRefreshLayout.isRefreshing = false
                    showWarningAlerter(resources.getString(R.string.ant_no_internet))
                }
            }
        }

        placeHolderRV.layoutManager = LinearLayoutManager(context)

        ivClose.setOnClickListener { activity?.finish() }
        viewBEChoice.setOnClickListener { viewModel.onLogoPressed() }

        ReceivingVideosManager.setReceivingVideoCallback(viewModel)
        if (refreshVODs && viewModel.userAuthorized()) {
            viewModel.refreshVODs()
            refreshVODs = false
        }
    }

    private fun initRecyclerView(
        adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>,
        recyclerView: RecyclerView
    ) {
        recyclerView.adapter = adapter
        val dividerItemDecoration = VerticalSpaceItemDecorator(
            dp2px(context!!, 32f).toInt()
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

    private fun triggerNewLiveButton(isVisible: Boolean) {
        if (isVisible && !isNewLiveButtonShown) {
            isNewLiveButtonShown = true
            btnNewLive.animate().translationYBy(150f).setDuration(300).start()
        } else if (!isVisible && isNewLiveButtonShown) {
            isNewLiveButtonShown = false
            btnNewLive.animate().translationY(0f).setDuration(300).start()
        }
    }

    private fun showLoadingListPlaceholder() {
        videosRV.visibility = View.INVISIBLE
        placeHolderRV.visibility = View.VISIBLE
    }

    private fun hidePlaceholder() {
        tvTitle.visibility = View.VISIBLE
        tvNoContent.visibility = View.INVISIBLE
        placeHolderRV.animate()
            .alpha(0.0f)
            .setDuration(600)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    placeHolderRV?.visibility = View.INVISIBLE
                }
            })
        videosRV.visibility = View.VISIBLE
    }

    private val networkStateObserver: Observer<NetworkConnectionState> = Observer { networkState ->
        when (networkState?.ordinal) {
            NetworkConnectionState.LOST.ordinal -> {
                if (!Global.networkAvailable) {
                    context?.resources?.getString(R.string.ant_no_internet)
                        ?.let { messageToDisplay ->
                            Handler().postDelayed({
                                showWarningAlerter(messageToDisplay)
                            }, 500)
                        }
                }
            }
            NetworkConnectionState.AVAILABLE.ordinal -> {
                viewModel.onNetworkGained()
            }
        }
    }
}
