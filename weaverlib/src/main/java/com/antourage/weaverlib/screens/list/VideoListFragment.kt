package com.antourage.weaverlib.screens.list


import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.di.injector
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.replaceFragment
import com.antourage.weaverlib.screens.base.BaseFragment
import com.antourage.weaverlib.screens.list.rv.VideosAdapter
import com.antourage.weaverlib.screens.list.rv.VideosLayoutManager
import com.antourage.weaverlib.screens.vod.VideoFragment
import com.antourage.weaverlib.screens.weaver.WeaverFragment
import kotlinx.android.synthetic.main.fragment_videos_list.*
import com.antourage.weaverlib.other.dp2px
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog
import com.antourage.weaverlib.screens.list.rv.VerticalSpaceItemDecorator


class VideoListFragment : Fragment() {

    lateinit var videoAdapter: VideosAdapter
    protected lateinit var viewModel: VideoListViewModel

    companion object {
        fun newInstance(): VideoListFragment {
            return VideoListFragment()
        }
    }
    //region Observers
    private val streamsObserver: Observer<List<StreamResponse>> = Observer { list ->
        if (list != null)
            videoAdapter.setStreamList(list)
        videoRefreshLayout.isRefreshing = false

    }
    private val beChoiceObserver: Observer<Boolean> = Observer {
        if(it != null && it)
            context?.let { context -> DevSettingsDialog(context, viewModel).show() }
    }

    //endregion


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate( R.layout.fragment_videos_list, container, false)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, activity?.injector?.getVideoListViewModelFactory())
            .get(VideoListViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi(view)
        subscribeToObservers()
    }

    private fun subscribeToObservers() {
        viewModel.listOfStreams.observe(this.viewLifecycleOwner, streamsObserver)
        viewModel.getShowBeDialog().observe(this.viewLifecycleOwner, beChoiceObserver)
    }

    override fun onStop() {
        super.onStop()
        viewModel.onStop()
    }

    override fun onResume() {
        super.onResume()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        viewModel.getStreams()
    }

     fun initUi(view: View?) {
        val onClick: (stream: StreamResponse) -> Unit = {
            if (it.isLive) {
                replaceFragment(WeaverFragment.newInstance(it), R.id.mainContent, true)
            } else {
                UserCache.newInstance().saveVideoToSeen(context!!, it.streamId)
                replaceFragment(VideoFragment.newInstance(it), R.id.mainContent, true)
            }
        }
        videoAdapter = VideosAdapter(onClick)
        videosRV.adapter = videoAdapter
        videosRV.layoutManager = VideosLayoutManager(context)
        val dividerItemDecoration = VerticalSpaceItemDecorator(
            dp2px(context!!,30f).toInt()
        )
        videosRV.addItemDecoration(dividerItemDecoration)
        videoRefreshLayout.setOnRefreshListener {
            viewModel.getStreams()
        }
        ivClose.setOnClickListener { activity?.finish() }
        viewBEChoice.setOnClickListener { viewModel.onLogoPressed() }
    }

//    override fun onNetworkConnectionLost() {
//        super.onNetworkConnectionLost()
//        viewModel.getListOfVideos()
//    }
}
