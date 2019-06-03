package com.antourage.weaverlib.screens.base.chat

import android.arch.lifecycle.Observer
import android.content.res.Configuration
import android.support.v7.widget.RecyclerView
import android.view.View
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.screens.base.streaming.StreamingFragment
import com.antourage.weaverlib.screens.chat.rv.MessagesAdapter
import com.antourage.weaverlib.screens.vod.rv.ChatLayoutManager


abstract class ChatFragment<VM : ChatViewModel> : StreamingFragment<VM>(){

    //region Observers
    private val messagesObserver: Observer<List<Message>> = Observer { list ->
        if (list != null) {
            (rvMessages.adapter as MessagesAdapter).setMessageList(list)
        }
    }
    //endregion

    private lateinit var rvMessages:RecyclerView

    override fun initUi(view: View?) {
        super.initUi(view)
        if (view != null) {
            rvMessages = view.findViewById(R.id.rvMessages)
            initMessagesRV()
        }
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
                MessagesAdapter(viewModel.getMessagesLiveData().value!!, Configuration.ORIENTATION_LANDSCAPE)
        } else if (newOrientation == Configuration.ORIENTATION_PORTRAIT) {
            rvMessages.isVerticalFadingEdgeEnabled = false
            rvMessages.adapter =
                MessagesAdapter(viewModel.getMessagesLiveData().value!!, Configuration.ORIENTATION_PORTRAIT)

        }
    }

}