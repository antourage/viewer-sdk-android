package com.antourage.weaverlib.screens.list


import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.observeSafe
import com.antourage.weaverlib.other.replaceFragment
import com.antourage.weaverlib.screens.base.BaseFragment
import com.antourage.weaverlib.screens.videos.rv.VideosAdapter
import com.antourage.weaverlib.screens.vod.VideoFragment
import com.antourage.weaverlib.screens.weaver.WeaverFragment
import kotlinx.android.synthetic.main.fragment_videos_list.*


class VideoListFragment : BaseFragment<VideoListViewModel>() {

    lateinit var videoAdapter: VideosAdapter

    companion object{
        fun newInstance():VideoListFragment{
            return VideoListFragment()
        }
    }

    private val streamsObserver: Observer<List<StreamResponse>> = Observer { list ->
        if(list != null)
            videoAdapter.setStreamList(list)
        videoRefreshLayout.isRefreshing = false

    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_videos_list
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(VideoListViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToObservers()
    }

    private fun subscribeToObservers() {
        viewModel.listOfStreams.observeSafe(this.viewLifecycleOwner,streamsObserver)
    }
    override fun onStop(){
        super.onStop()
        viewModel.onStop()
    }

    override fun onResume(){
        super.onResume()
        viewModel.getStreams()
    }
    override fun initUi(view: View?) {
        val onClick:(stream:StreamResponse)->Unit = {
            if (it.isLive){
                replaceFragment(WeaverFragment.newInstance(it),R.id.mainContent,true)
            }else {
                UserCache.newInstance().saveVideoToSeen(context!!, it.streamId)
                replaceFragment(VideoFragment.newInstance(it),R.id.mainContent,true)
            }
        }
        videoAdapter = VideosAdapter(onClick)
        videosRV.adapter = videoAdapter
        videosRV.layoutManager = LinearLayoutManager(context)
        videoRefreshLayout.setOnRefreshListener {
            viewModel.getStreams()
        }
        ivClose.setOnClickListener { activity?.finish() }
    }


}
