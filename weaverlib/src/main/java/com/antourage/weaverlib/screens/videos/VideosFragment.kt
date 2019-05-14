package com.antourage.weaverlib.screens.videos


import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.replaceFragment
import com.antourage.weaverlib.screens.base.BaseFragment
import com.antourage.weaverlib.screens.videos.rv.VideosAdapter
import com.antourage.weaverlib.screens.weaver.WeaverFragment
import kotlinx.android.synthetic.main.fragment_videos.*


class VideosFragment : BaseFragment<VideosViewModel>() {

    lateinit var videoAdapter: VideosAdapter

    companion object{
        fun newInstance():VideosFragment{
            return VideosFragment()
        }
    }

    private val streamsObserver: Observer<List<StreamResponse>> = Observer { list ->
        if(list != null)
            videoAdapter.setStreamList(list)
        videoRefreshLayout.isRefreshing = false

    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_videos
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(VideosViewModel::class.java)
        subscribeToObservers()
    }

    private fun subscribeToObservers() {
        viewModel.listOfStreams.removeObserver { streamsObserver }
        viewModel.listOfStreams.observe(this,streamsObserver)
    }

    override fun initUi(view: View?) {
        val onClick:(stream:StreamResponse)->Unit = {
            UserCache.newInstance().saveVideoToSeen(context!!,it.streamId)
            replaceFragment(WeaverFragment.newInstance(it),R.id.mainContent,true)
        }
        videoAdapter = VideosAdapter(onClick)
        videosRV.adapter = videoAdapter
        videosRV.layoutManager = LinearLayoutManager(context)
        viewModel.getStreams()
        videoRefreshLayout.setOnRefreshListener {
            viewModel.getStreams()
        }
    }


}
