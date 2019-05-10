package com.antourage.weaverlib.screens.base

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.AnimRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.observeSafe

abstract class BaseFragment<VM:BaseViewModel>:Fragment(){

    protected lateinit var viewModel: VM

    protected abstract fun getLayoutId(): Int

    protected abstract fun initUi(view: View?)

    // region observers
    private val errorObserver:androidx.lifecycle.Observer<String?> = androidx.lifecycle.Observer { s:String? ->
        if (s != null) {
            showErrorAlerter(s)
            BaseViewModel.error.postValue(null)
        }
    }


    private val warningObserver:androidx.lifecycle.Observer<String?> = androidx.lifecycle.Observer{ s:String? ->
        if (s != null) {
            showWarningAlerter(s)
            BaseViewModel.warning.postValue(null)
        }
    }

    private val successObserver:androidx.lifecycle.Observer<String?> = androidx.lifecycle.Observer{ s:String? ->
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
        BaseViewModel.error.observeSafe(this, errorObserver)

        BaseViewModel.warning.observeSafe(this, warningObserver)

        BaseViewModel.success.observeSafe(this, successObserver)

    }

    private fun showErrorAlerter(s: String) {
        Toast.makeText(context,s,Toast.LENGTH_LONG).show()
    }
    private fun showWarningAlerter(s: String) {
        Toast.makeText(context,s,Toast.LENGTH_LONG).show()
    }
    private fun showSuccessAlerter(s: String) {
        Toast.makeText(context,s,Toast.LENGTH_LONG).show()
    }

}