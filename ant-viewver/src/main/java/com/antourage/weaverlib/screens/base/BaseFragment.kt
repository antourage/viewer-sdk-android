package com.antourage.weaverlib.screens.base

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat.getSystemService
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager

internal abstract class BaseFragment<VM : BaseViewModel> : Fragment() {

    protected lateinit var viewModel: VM

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
}
