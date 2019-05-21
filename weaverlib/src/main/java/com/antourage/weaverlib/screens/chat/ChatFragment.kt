package com.antourage.weaverlib.screens.chat

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.other.observeSafe
import com.antourage.weaverlib.screens.base.BaseFragment
import com.antourage.weaverlib.screens.chat.rv.MessagesAdapter
import kotlinx.android.synthetic.main.fragment_chat.*


class ChatFragment : BaseFragment<ChatViewModel>() {

    companion object {
        const val ARGS_IS_LIVE = "args_is_live"
        const val ARGS_STREAM_ID = "args_stream_id"

        fun newInstance(id: Int, isLive: Boolean): ChatFragment {
            val bundle = Bundle()
            bundle.putInt(ARGS_STREAM_ID, id)
            bundle.putBoolean(ARGS_IS_LIVE, isLive)
            val fragment = ChatFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_chat
    }

    //region Observers
    private val messagesObserver: Observer<List<Message>> = Observer { list ->
        if (list != null) {
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
        viewModel.getMessagesLiveData().observeSafe(this, messagesObserver)
    }

    override fun initUi(view: View?) {
        initMessagesRV()
        arguments?.let {
            if (!it.getBoolean(ARGS_IS_LIVE, false)) {
                btnSend.visibility = View.GONE
                etMessage.visibility = View.GONE
                deviderChat.visibility = View.GONE
            }
        }
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
            rvMessages.adapter =
                MessagesAdapter(viewModel.getMessagesLiveData().value!!, Configuration.ORIENTATION_LANDSCAPE)
        } else if (newOrientation == Configuration.ORIENTATION_PORTRAIT) {
            rvMessages.isVerticalFadingEdgeEnabled = false
            rvMessages.adapter =
                MessagesAdapter(viewModel.getMessagesLiveData().value!!, Configuration.ORIENTATION_PORTRAIT)
        }
    }


}
