package com.antourage.weaverlib.screens.base.chat

import android.arch.lifecycle.Observer
import android.content.res.Configuration
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.support.design.widget.NavigationView
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.other.ui.CustomDrawerLayout
import com.antourage.weaverlib.screens.base.streaming.StreamingFragment
import com.antourage.weaverlib.screens.vod.rv.MessagesAdapter
import com.antourage.weaverlib.screens.vod.rv.ChatLayoutManager
import kotlinx.android.synthetic.main.fragment_weaver_portrait.*


abstract class ChatFragment<VM : ChatViewModel> : StreamingFragment<VM>(),CustomDrawerLayout.DrawerTouchListener {

    //region Observers
    private val messagesObserver: Observer<List<Message>> = Observer { list ->
        if (list != null) {
            (rvMessages.adapter as MessagesAdapter).setMessageList(list)
        }
    }
    //endregion

    private lateinit var rvMessages:RecyclerView
    private lateinit var drawerLayout: CustomDrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var llMessageWrapper: LinearLayout

    override fun onDrawerTouch() {
        handleControlsVisibility()
    }

    override fun initUi(view: View?) {
        super.initUi(view)
        if (view != null) {
            drawerLayout = view.findViewById(R.id.drawerLayout)
            drawerLayout.touchListener = this
            navigationView = view.findViewById(R.id.navView)
            rvMessages = view.findViewById(R.id.rvMessages)
            llMessageWrapper = view.findViewById(R.id.ll_wrapper)
            initMessagesRV()
            initNavigationView()
        }
    }
    private fun initNavigationView(){
        drawerLayout.setScrimColor(Color.TRANSPARENT)
        drawerLayout.openDrawer(navigationView)
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN)
        navigationView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (activity != null) {
                    val drawerToggle = object : ActionBarDrawerToggle(activity, drawerLayout, null, 0, 0) {
                        override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                            super.onDrawerSlide(drawerView, slideOffset)
                            val orientation = resources.configuration.orientation
                            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                                llMessageWrapper.alpha = slideOffset
                                if (slideOffset == 0.0f) {
                                    etMessage.isEnabled = false
                                }
                                if (slideOffset == 1.0f) {
                                    etMessage.isEnabled = true
                                }
                            } else{
                                drawerLayout.openDrawer(navigationView,false)
                            }
                        }
                    }
                    drawerLayout.addDrawerListener(drawerToggle)
                    navigationView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        })
    }
    private fun initMessagesRV() {
        val linearLayoutManager = ChatLayoutManager(
            context
        )
        linearLayoutManager.stackFromEnd = true
        rvMessages.overScrollMode = View.OVER_SCROLL_NEVER
        rvMessages.isVerticalFadingEdgeEnabled = false
        rvMessages.layoutManager = linearLayoutManager
        rvMessages.adapter = MessagesAdapter(listOf(), Configuration.ORIENTATION_PORTRAIT)
    }
    override fun subscribeToObservers() {
        viewModel.getMessagesLiveData().observe(this.viewLifecycleOwner, messagesObserver)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val newOrientation = newConfig.orientation
        if (newOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            rvMessages.isVerticalFadingEdgeEnabled = true
            rvMessages.adapter =
                MessagesAdapter(listOf(), Configuration.ORIENTATION_LANDSCAPE)
            (rvMessages.adapter as MessagesAdapter).setMessageList(viewModel.getMessagesLiveData().value!!)
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        } else if (newOrientation == Configuration.ORIENTATION_PORTRAIT) {
            rvMessages.isVerticalFadingEdgeEnabled = false
            rvMessages.adapter =
                MessagesAdapter(listOf(), Configuration.ORIENTATION_PORTRAIT)
            (rvMessages.adapter as MessagesAdapter).setMessageList(viewModel.getMessagesLiveData().value!!)
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN)
        }
        Handler(Looper.getMainLooper()).post {
            drawerLayout.openDrawer(navigationView)
        }
    }


}