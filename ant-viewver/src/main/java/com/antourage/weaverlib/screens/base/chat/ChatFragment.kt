package com.antourage.weaverlib.screens.base.chat

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.view.View
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.dp2px
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.other.orientation
import com.antourage.weaverlib.other.ui.ChatItemDecoratorLandscape
import com.antourage.weaverlib.other.ui.CustomDrawerLayout
import com.antourage.weaverlib.other.ui.MarginItemDecoration
import com.antourage.weaverlib.screens.base.AntourageActivity
import com.antourage.weaverlib.screens.base.chat.rv.MessagesAdapter
import com.antourage.weaverlib.screens.base.player.BasePlayerFragment
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.fragment_player_live_video_portrait.*

internal abstract class ChatFragment<VM : ChatViewModel> : BasePlayerFragment<VM>(),
    CustomDrawerLayout.DrawerTouchListener {

    private var isChatDismissed: Boolean = false

    private lateinit var rvMessages: RecyclerView
    private lateinit var drawerLayout: CustomDrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var llMessageWrapper: LinearLayout

    private val messagesObserver: Observer<List<Message>> = Observer { list ->
        if (list != null) {
            rvMessages.apply {
                val shouldScrollToBottom =
                    ((adapter as MessagesAdapter).newMessagesWereAdded(list) &&
                            userIsAtTheBottomOfTheChat())
                (adapter as MessagesAdapter).setMessageList(list)
                if (shouldScrollToBottom) {
                    adapter?.itemCount?.let { scrollToPosition(it - 1) }
                }
            }
        }
    }

    private fun userIsAtTheBottomOfTheChat(): Boolean {
        val layoutManager = rvMessages.layoutManager
        if (layoutManager is LinearLayoutManager) {
            if (layoutManager.findLastCompletelyVisibleItemPosition() == layoutManager.itemCount - 1)
                return true
        }
        return false
    }

    /**
     * as drawer intercepts touch events, event when it's closed,  we need to provide
     * the possibility to toggle player controls on single tap
     */
    override fun onDrawerSingleClick() {
        toggleControlsVisibility()
    }

    override fun initUi(view: View?) {
        super.initUi(view)
        view?.apply {
            drawerLayout = findViewById(R.id.drawerLayout)
            drawerLayout.touchListener = this@ChatFragment

            navigationView = findViewById(R.id.navView)
            rvMessages = findViewById(R.id.rvMessages)
            llMessageWrapper = findViewById(R.id.ll_wrapper)
        }
        initMessagesRV()
        initAndOpenNavigationDrawer()
    }

    override fun subscribeToObservers() {
        viewModel.getMessagesLiveData().observe(this.viewLifecycleOwner, messagesObserver)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val newOrientation = newConfig.orientation
        if (newOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (isChatDismissed) {
                (activity as AntourageActivity).hideSoftKeyboard()
                drawerLayout.closeDrawer(navView)
            }
            context?.let { setLandscapeUI(it) }
        } else if (newOrientation == Configuration.ORIENTATION_PORTRAIT) {
            rvMessages.isVerticalFadingEdgeEnabled = false
            rvMessages.adapter =
                MessagesAdapter(mutableListOf(), Configuration.ORIENTATION_PORTRAIT)
            updateMessagesList(viewModel.getMessagesLiveData().value!!)
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN)
            context?.let { initMessagesDivider(it, false) }
        }
    }

    private fun initAndOpenNavigationDrawer() {
        drawerLayout.apply {
            setScrimColor(Color.TRANSPARENT)
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN)

            openDrawer(navigationView)
        }
        navigationView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (activity != null) {
                    val drawerToggle =
                        object : ActionBarDrawerToggle(activity, drawerLayout, null, 0, 0) {
                            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                                super.onDrawerSlide(drawerView, slideOffset)
                                val orientation = resources.configuration.orientation
                                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                                    llMessageWrapper.alpha = slideOffset
                                    if (slideOffset == 0.0f) {
                                        etMessage.isEnabled = false
                                        isChatDismissed = true
                                    }
                                    if (slideOffset == 1.0f) {
                                        etMessage.isEnabled = true
                                        isChatDismissed = false
                                    }
                                } else {
                                    drawerLayout.openDrawer(navigationView, false)
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
        val linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.stackFromEnd = true
        rvMessages.apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalFadingEdgeEnabled = false
            layoutManager = linearLayoutManager
            adapter = MessagesAdapter(mutableListOf(), Configuration.ORIENTATION_PORTRAIT)
            initMessagesDivider(context, orientation() == Configuration.ORIENTATION_LANDSCAPE)
        }
    }

    private fun setLandscapeUI(context: Context) {
        rvMessages.apply {
            isVerticalFadingEdgeEnabled = true
            adapter = MessagesAdapter(mutableListOf(), Configuration.ORIENTATION_LANDSCAPE)

            val messages = viewModel.getMessagesLiveData().value
            updateMessagesList(messages?.let { it } ?: listOf())
        }
        initMessagesDivider(context, true)
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    private fun initMessagesDivider(context: Context, landscape: Boolean) {
        val dividerDecorator = if (landscape) ChatItemDecoratorLandscape(
            dp2px(context, 5f).toInt(),
            dp2px(context, 12f).toInt(),
            dp2px(context, 0f).toInt(),
            dp2px(context, 12f).toInt(),
            dp2px(context, 5f).toInt()
        ) else MarginItemDecoration(
            dp2px(context, 20f).toInt()
        )
        rvMessages.apply {
            if (itemDecorationCount > 0)
                removeItemDecorationAt(0)
            addItemDecoration(dividerDecorator)
        }
    }

    private fun updateMessagesList(list: List<Message>) {
        rvMessages.apply {
            val recyclerViewState = layoutManager?.onSaveInstanceState()
            (adapter as MessagesAdapter).setMessageList(list)
            layoutManager?.onRestoreInstanceState(recyclerViewState)
        }
    }
}
