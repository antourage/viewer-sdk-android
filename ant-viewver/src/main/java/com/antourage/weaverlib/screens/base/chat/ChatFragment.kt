package com.antourage.weaverlib.screens.base.chat

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.view.View
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.animateShowHide
import com.antourage.weaverlib.other.dp2px
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.other.models.MessageType
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
    private var newCommentsButton: ConstraintLayout? = null
    //llMessageWrapper not in use in VOD
    private var llMessageWrapper: ConstraintLayout? = null

    private val messagesObserver: Observer<List<Message>> = Observer { list ->
        val filteredList = list?.filter { it.type == MessageType.USER }
        if (filteredList != null && filteredList.isNotEmpty()) {
            (rvMessages.adapter as MessagesAdapter?)?.let { adapter ->
                val numOfNew = adapter.amountOfNewMessagesWillBeAdd(filteredList)
                val shouldScrollToBottom = (numOfNew > 0)  && userIsAtTheBottomOfTheChat()

                adapter.setMessageList(filteredList, true)
                if (shouldScrollToBottom) {
                    adapter.itemCount.let { rvMessages.scrollToPosition(it - 1) }
                    viewModel.setSeenComments(isAll = true)
                }
                if (viewModel.checkIfMessageByUser(filteredList.last().userID)){
                    viewModel.setSeenComments(isAll = true)
                } else if (!shouldScrollToBottom && numOfNew > 0){
                    viewModel.addUnseenComments(numOfNew)
                }
            }
        }
    }

    private val unseenCommentsObserver: Observer<Int> = Observer { quantity ->
        if ((quantity ?:0) == 0){
            switchNewCommentsVisibility(false)
        } else {
            updateNewCommentsQuantity(quantity)
            switchNewCommentsVisibility(true)
        }
    }

    private fun updateNewCommentsQuantity(quantity: Int) {
        newCommentsButton?.findViewById<TextView>(R.id.new_comment_text)?.text = resources
            .getQuantityString(R.plurals.ant_number_new_comments, quantity, quantity)
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
     * Should be used when there are new comments.
     * @return number of new just seen comments or 0 if there are no new comments seen.
     */
    private fun checkNumOfSeenComments(): Int {
        val layoutManager = rvMessages.layoutManager as LinearLayoutManager
        val numOfWatched = (layoutManager.findLastVisibleItemPosition() + 1) -
                (layoutManager.itemCount - viewModel.getUnseenQuantity())
        return if (numOfWatched > 0) numOfWatched else 0
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
            drawerLayout.drawerElevation = 0f

            navigationView = findViewById(R.id.navView)
            rvMessages = findViewById(R.id.rvMessages)
            llMessageWrapper = findViewById(R.id.ll_wrapper)
            newCommentsButton = findViewById(R.id.bttn_new_comments)
        }
        initMessagesRV()
        initOnScrollRVListener()
        initAndOpenNavigationDrawer()
        newCommentsButton?.setOnClickListener {
            rvMessages.adapter?.let {rvMessages.smoothScrollToPosition(it.itemCount - 1) }
        }
    }

    override fun subscribeToObservers() {
        viewModel.getMessagesLiveData().observe(this.viewLifecycleOwner, messagesObserver)
        viewModel.getNewUnseenComments().observe(this.viewLifecycleOwner, unseenCommentsObserver)
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
            /*rvMessages.adapter =
                MessagesAdapter(mutableListOf(), Configuration.ORIENTATION_PORTRAIT, viewModel.getStartTime())
            updateMessagesList(viewModel.getMessagesLiveData().value!!)*/
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN)
            context?.let { initMessagesDivider(it, false) }
        }
    }

    fun setStartTime(startTime: Long) {
        viewModel.setStartTime(startTime)
        rvMessages.adapter = MessagesAdapter(mutableListOf(), orientation(), viewModel.getStartTime())
        viewModel.getMessagesLiveData().value?.let {updateMessagesList(it)}
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
                                    llMessageWrapper?.alpha = slideOffset
                                    if (slideOffset == 0.0f) {
                                        etMessage?.isEnabled = false
                                        isChatDismissed = true
                                    }
                                    if (slideOffset == 1.0f) {
                                        etMessage?.isEnabled = true
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
            adapter = MessagesAdapter(mutableListOf(), Configuration.ORIENTATION_PORTRAIT, viewModel.getStartTime())
            initMessagesDivider(context, orientation() == Configuration.ORIENTATION_LANDSCAPE)
        }
    }

    private fun initOnScrollRVListener() {
        rvMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (newCommentsButton?.visibility == View.VISIBLE) {
                    if (userIsAtTheBottomOfTheChat()) {
                        viewModel.setSeenComments(isAll = true)
                    } else {
                        val newSeenComments = checkNumOfSeenComments()
                        if (newSeenComments > 0){
                            viewModel.setSeenComments(numOfSeen = newSeenComments)
                        }
                    }
                }
            }
        })
    }

    private fun switchNewCommentsVisibility(shouldShow: Boolean){
        newCommentsButton?.animateShowHide(shouldShow)
    }

    private fun setLandscapeUI(context: Context) {
        rvMessages.apply {
            isVerticalFadingEdgeEnabled = true
            /*adapter = MessagesAdapter(mutableListOf(), Configuration.ORIENTATION_LANDSCAPE, viewModel.getStartTime())

            val messages = viewModel.getMessagesLiveData().value
            updateMessagesList(messages?.let { it } ?: listOf())*/
        }
        initMessagesDivider(context, true)
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    private fun initMessagesDivider(context: Context, landscape: Boolean) {
        val dividerDecorator = if (landscape) ChatItemDecoratorLandscape(
            dp2px(context, 12f).toInt(),
            dp2px(context, 12f).toInt(),
            dp2px(context, 0f).toInt(),
            dp2px(context, 12f).toInt(),
            dp2px(context, 12f).toInt()
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
