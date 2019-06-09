package com.antourage.weaverlib.screens.base

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.antourage.weaverlib.other.observeSafe

abstract class BaseFragment<VM : BaseViewModel> : Fragment() {

    protected lateinit var viewModel: VM

    protected abstract fun getLayoutId(): Int

    protected abstract fun initUi(view: View?)

    // region observers
    private val errorObserver: Observer<String?> = Observer { s: String? ->
        if (s != null) {
            showErrorAlerter(s)
            BaseViewModel.error.postValue(null)
        }
    }


    private val warningObserver: Observer<String?> = Observer { s: String? ->
        if (s != null) {
            showWarningAlerter(s)
            BaseViewModel.warning.postValue(null)
        }
    }

    private val successObserver: Observer<String?> = Observer { s: String? ->
        if (s != null) {
            showSuccessAlerter(s)
            BaseViewModel.success.postValue(null)
        }
    }
    // endregion

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(getLayoutId(), container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (activity != null) {
            initUi(getView())
        }
        BaseViewModel.error.observe(this.viewLifecycleOwner, errorObserver)

        BaseViewModel.warning.observe(this.viewLifecycleOwner, warningObserver)

        BaseViewModel.success.observe(this.viewLifecycleOwner, successObserver)

    }

    private fun showErrorAlerter(s: String) {
       // Toast.makeText(context, s, Toast.LENGTH_LONG).show()
        var alertDialog = AlertDialog.Builder(context!!) // this: Activity
            .setMessage(s)
            .create()

        alertDialog.show()
    }

    private fun showWarningAlerter(s: String) {
        Toast.makeText(context, s, Toast.LENGTH_LONG).show()
    }

    private fun showSuccessAlerter(s: String) {
        Toast.makeText(context, s, Toast.LENGTH_LONG).show()
    }

}