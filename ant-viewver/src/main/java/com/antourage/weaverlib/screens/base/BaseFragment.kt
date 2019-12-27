package com.antourage.weaverlib.screens.base

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import com.antourage.weaverlib.R
import com.tapadoo.alerter.Alerter

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
            showWarningAlerter(context.resources.getString(R.string.ant_no_internet))
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

    protected fun showWarningAlerter(alerterText: String) {
        Alerter.create(this.activity)
            .setBackgroundColorRes(R.color.ant_pink)
            .setTitle(alerterText)
            .show()
    }
}
