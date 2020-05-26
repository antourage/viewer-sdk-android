package com.antourage.weaverlib.screens.base

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.antourage.weaverlib.R
import com.antourage.weaverlib.screens.list.VideoListFragment

internal abstract class BaseFragment<VM : BaseViewModel> : Fragment() {

    protected lateinit var viewModel: VM

    protected var keyboardIsVisible = false
        get() {
            return if (activity != null) (activity as AntourageActivity).keyboardIsVisible
            else false
        }
        private set

    private val messageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if(this@BaseFragment !is VideoListFragment){
                showNoInternetMessage()
            }
        }
    }

    protected abstract fun getLayoutId(): Int

    protected abstract fun initUi(view: View?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context?.let {
            LocalBroadcastManager.getInstance(it).registerReceiver(
                messageReceiver,
                IntentFilter(it.resources.getString(R.string.ant_no_internet_action))
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(getLayoutId(), container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (activity != null) {
            initUi(getView())
        }
    }

    override fun onDestroy() {
        context?.let {
            LocalBroadcastManager.getInstance(it).unregisterReceiver(messageReceiver)
        }
        super.onDestroy()
    }

    protected fun hideKeyboard() {
        val view = view?.rootView?.windowToken
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view, 0)
    }

    protected fun hideKeyboard(withCallback: Boolean = true) {
        activity?.let { (it as AntourageActivity).triggerKeyboardCallback(withCallback) }

        val view = activity?.currentFocus
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    protected fun showKeyboard(view: View, withCallback: Boolean = true) {
        activity?.let { (it as AntourageActivity).triggerKeyboardCallback(withCallback) }

        if (view.requestFocus()) {
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm!!.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    open fun onShowKeyboard(keyboardHeight: Int) {}
    open fun onHideKeyboard(keyboardHeight: Int) {}

    open fun showNoInternetMessage(){}
}
