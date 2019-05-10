package com.antourage.weaverlib.screens.weaver


import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.OrientationEventListener
import android.view.View
import android.view.WindowManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.screens.base.BaseFragment
import com.antourage.weaverlib.screens.weaver.rv.MessagesAdapter
import com.google.android.exoplayer2.Player
import kotlinx.android.synthetic.main.fragment_weaver_portrait.*
import kotlinx.android.synthetic.main.player_custom_control.*

class WeaverFragment : BaseFragment<WeaverViewModel>() {

    private lateinit var orientationEventListener: OrientationEventListener
    private var loader: AnimatedVectorDrawableCompat? = null

    companion object {
        const val ARGS_STREAM = "args_stream"

        fun newInstance(stream: StreamResponse): WeaverFragment {
            val bundle = Bundle()
            bundle.putParcelable(ARGS_STREAM, stream)
            val fragment = WeaverFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_weaver_portrait
    }

    //region Observers

    private val streamStateObserver: Observer<Int> = Observer { state ->
        if (ivLoader != null)
            when (state) {
                Player.STATE_READY -> hideLoading()
                Player.STATE_BUFFERING -> showLoading()
            }
    }

    private val messagesObserver:Observer<List<Message>> = Observer {list->
        if(list != null){
            (rvMessages.adapter as MessagesAdapter).setMessageList(list)
        }
    }

    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(WeaverViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        constraintLayoutParent.loadLayoutDescription(R.xml.cl_states_player_screen)
        subscribeToObservers()
    }

    private fun subscribeToObservers() {
        viewModel.getPlaybackState().removeObserver(streamStateObserver)
        viewModel.getPlaybackState().observe(this, streamStateObserver)
        viewModel.getMessagesLiveData().removeObserver(messagesObserver)
        viewModel.getMessagesLiveData().observe(this,messagesObserver)
    }

    override fun initUi(view: View?) {
        playerView.player = viewModel.getExoPlayer(arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.hlsUrl)
        initLoader()
        initOrientationHandling()
        initMessagesRV()
        btnSend.setOnClickListener {
            viewModel.addMessage(etMessage.text.toString())
            etMessage.setText("")
        }
        tvStreamName.text = arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.streamTitle
        tvBroadcastedBy.text = arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.creatorFullname
    }

    private fun initMessagesRV() {
        val linearLayoutManager = LinearLayoutManager(
            context, RecyclerView.VERTICAL, false
        )
        linearLayoutManager.stackFromEnd = true
        rvMessages.layoutManager = linearLayoutManager
        rvMessages.adapter = MessagesAdapter(listOf(),Configuration.ORIENTATION_PORTRAIT)
    }

    private fun initLoader() {
        if (context != null) {
            loader = AnimatedVectorDrawableCompat.create(context!!, R.drawable.loader_logo)
            ivLoader.setImageDrawable(loader)
        }
        showLoading()
    }

    private fun showLoading() {
        if (loader != null &&!loader!!.isRunning) {
            loader?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    ivLoader.post { loader?.start() }
                }
            })
            loader?.start()
            ivLoader.visibility = View.VISIBLE
        }
    }

    protected fun hideLoading() {
        if (ivLoader.getVisibility() == View.VISIBLE &&(loader!= null && loader!!.isRunning())) {
            ivLoader.setVisibility(View.GONE)
            loader?.clearAnimationCallbacks()
            loader?.stop()
        }
    }

    private fun initOrientationHandling() {
        orientationEventListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                val epsilon = 10
                val leftLandscape = 90
                val rightLandscape = 270
                if (epsilonCheck(orientation, leftLandscape, epsilon) ||
                    epsilonCheck(orientation, rightLandscape, epsilon)
                ) {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                }

            }
        }
        ivScreenSize.setOnClickListener {
            val currentOrientation = activity?.resources?.configuration?.orientation
            if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    private fun epsilonCheck(a: Int, b: Int, epsilon: Int): Boolean {
        return a > b - epsilon && a < b + epsilon
    }

    override fun onResume() {
        super.onResume()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        orientationEventListener.enable()
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        orientationEventListener.disable()
        viewModel.onPause()

    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.releasePlayer()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val newOrientation = newConfig.orientation

        if (newOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            constraintLayoutParent.setState(R.id.constraintStateLandscape,newConfig.screenWidthDp,newConfig.screenHeightDp)
            rvMessages.adapter = MessagesAdapter(viewModel.getMessagesLiveData().value!!,Configuration.ORIENTATION_LANDSCAPE)
        } else if (newOrientation == Configuration.ORIENTATION_PORTRAIT) {
            constraintLayoutParent.setState(R.id.constraintStatePortrait,newConfig.screenWidthDp,newConfig.screenHeightDp)
            rvMessages.adapter = MessagesAdapter(viewModel.getMessagesLiveData().value!!,Configuration.ORIENTATION_PORTRAIT)
        }
    }
}
