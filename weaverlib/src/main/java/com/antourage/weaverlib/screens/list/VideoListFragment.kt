package com.antourage.weaverlib.screens.list

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.pm.ActivityInfo
import android.icu.util.ValueIterator
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.di.injector
import com.antourage.weaverlib.other.dp2px
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.replaceFragment
import com.antourage.weaverlib.other.ui.MyNestedScrollView
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog
import com.antourage.weaverlib.screens.list.rv.VerticalSpaceItemDecorator
import com.antourage.weaverlib.screens.list.rv.VideosAdapter
import com.antourage.weaverlib.screens.list.rv.VideosLayoutManager
import com.antourage.weaverlib.screens.vod.VodPlayerFragment
import com.antourage.weaverlib.screens.weaver.PlayerFragment
import kotlinx.android.synthetic.main.fragment_videos_list.*

class VideoListFragment : Fragment(), MyNestedScrollView.OnBottomReachedListener {

    private lateinit var viewModel: VideoListViewModel
    private lateinit var videoAdapter: VideosAdapter
    private lateinit var placeHolderAdapter: VideoPlaceholdersAdapter
    private lateinit var rvLayoutManager: VideosLayoutManager

    companion object {
        fun newInstance(): VideoListFragment {
            return VideoListFragment()
        }
    }

    //region Observers
    private val streamsObserver: Observer<List<StreamResponse>> = Observer { list ->
        list?.let {
            if (list.isNullOrEmpty()) {
                showEmptyListPlaceholder()
            } else {
                hidePlaceholder()
                videoAdapter.setStreamList(it)
                nestedSV.setBottomReachesListener(this@VideoListFragment)
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
        viewModel.refreshVODsLocally()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    override fun onBottomReached(view: View?) {
        val total = rvLayoutManager.itemCount
        val lastVisibleItem = rvLayoutManager.findLastCompletelyVisibleItemPosition()
        if (total <= lastVisibleItem + 1 && videoAdapter.getStreams()[lastVisibleItem].id == -2) {
            viewModel.refreshVODs()
        }
    }

    private fun initUi() {
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
            viewModel.refreshVODs(0)
        }

        placeHolderRV.layoutManager = LinearLayoutManager(context)

        ivClose.setOnClickListener { activity?.finish() }
        viewBEChoice.setOnClickListener { viewModel.onLogoPressed() }
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

    }

    private fun showLoadingLayout() {

    }

    private fun showEmptyListPlaceholder() {
        nestedSV.visibility = View.INVISIBLE
        placeHolderNestedSV.visibility = View.VISIBLE
        tvTitle.visibility = View.INVISIBLE
        tvNoContent.visibility = View.VISIBLE
        placeHolderAdapter.setItems(
            arrayListOf(
                R.drawable.no_content_placeholder_1,
                R.drawable.no_content_placeholder_1,
                R.drawable.no_content_placeholder_1
            )
        )
    }

    private fun hidePlaceholder() {
        tvTitle.visibility = View.VISIBLE
        tvNoContent.visibility = View.INVISIBLE
        nestedSV.visibility = View.VISIBLE
        placeHolderNestedSV.visibility = View.INVISIBLE
    }
}
