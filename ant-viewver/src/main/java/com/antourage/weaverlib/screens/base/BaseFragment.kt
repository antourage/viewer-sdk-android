package com.antourage.weaverlib.screens.base

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager

internal abstract class BaseFragment<VM : BaseViewModel> : Fragment() {

    protected lateinit var viewModel: VM

    protected var keyboardIsVisible = false
        get() {
            return if (activity != null) (activity as AntourageActivity).keyboardIsVisible
            else false
        }
        private set

    protected abstract fun getLayoutId(): Int

    protected abstract fun initUi(view: View?)

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
}
