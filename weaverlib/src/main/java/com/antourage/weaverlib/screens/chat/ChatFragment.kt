package com.antourage.weaverlib.screens.chat

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.other.observeSafe
import com.antourage.weaverlib.screens.base.BaseFragment
import com.antourage.weaverlib.screens.chat.rv.MessagesAdapter
import kotlinx.android.synthetic.main.fragment_chat.*


class ChatFragment : BaseFragment<ChatViewModel>() {

    companion object {
        fun newInstance():ChatFragment{
            return ChatFragment()
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_chat
    }
    //region Observers
    private val messagesObserver: Observer<List<Message>> = Observer { list->
        if(list != null){
            (rvMessages.adapter as MessagesAdapter).setMessageList(list)
        }
    }

    //endregion
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(ChatViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToObservers()
    }

    private fun subscribeToObservers() {
        viewModel.getMessagesLiveData().observeSafe(this,messagesObserver)
    }

    override fun initUi(view: View?) {
        initMessagesRV()
        btnSend.setOnClickListener {
            viewModel.addMessage(etMessage.text.toString())
            etMessage.setText("")
        }
    }

    private fun initMessagesRV() {
        val linearLayoutManager = LinearLayoutManager(
            context, RecyclerView.VERTICAL, false
        )
        linearLayoutManager.stackFromEnd = true
        rvMessages.overScrollMode = View.OVER_SCROLL_NEVER
        rvMessages.isVerticalFadingEdgeEnabled = false
        rvMessages.layoutManager = linearLayoutManager
        rvMessages.adapter = MessagesAdapter(listOf(), Configuration.ORIENTATION_PORTRAIT)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val newOrientation = newConfig.orientation
        if (newOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            rvMessages.isVerticalFadingEdgeEnabled = true
            rvMessages.adapter = MessagesAdapter(viewModel.getMessagesLiveData().value!!,Configuration.ORIENTATION_LANDSCAPE)
        } else if (newOrientation == Configuration.ORIENTATION_PORTRAIT) {
            rvMessages.isVerticalFadingEdgeEnabled = false
            rvMessages.adapter = MessagesAdapter(viewModel.getMessagesLiveData().value!!,Configuration.ORIENTATION_PORTRAIT)
        }
    }



}
