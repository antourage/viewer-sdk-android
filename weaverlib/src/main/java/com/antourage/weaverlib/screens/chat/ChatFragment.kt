package com.antourage.weaverlib.screens.chat

import android.view.View

import com.antourage.weaverlib.R
import com.antourage.weaverlib.screens.base.BaseFragment


class ChatFragment : BaseFragment<ChatViewModel>() {

    companion object {
        fun newInstance():ChatFragment{
            return ChatFragment()
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_chat
    }

    override fun initUi(view: View?) {

    }



}
