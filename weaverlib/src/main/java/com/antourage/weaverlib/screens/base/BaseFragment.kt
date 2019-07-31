package com.antourage.weaverlib.screens.base

import android.arch.lifecycle.Observer
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.antourage.weaverlib.screens.base.AntourageActivity.Companion.ACTION_CONNECTION_AVAILABLE
import com.antourage.weaverlib.screens.base.AntourageActivity.Companion.ACTION_CONNECTION_LOST

abstract class BaseFragment<VM : BaseViewModel> : Fragment() {

    protected lateinit var viewModel: VM

    private val onConnectionLostReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onNetworkConnectionLost()
        }
    }
    private val onConnectionAvailableReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onNetworkConnectionAvailable()
        }
    }

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

    override fun onResume() {
        super.onResume()
        val connectionLostFilter = IntentFilter(ACTION_CONNECTION_LOST)
        val connectionAvailableFilter = IntentFilter(ACTION_CONNECTION_AVAILABLE)
        context?.let {
            LocalBroadcastManager.getInstance(it)
                .registerReceiver(onConnectionLostReceiver, connectionLostFilter)
            LocalBroadcastManager.getInstance(it)
                .registerReceiver(onConnectionAvailableReceiver, connectionAvailableFilter)
        }
    }

    override fun onPause(){
        super.onPause()
        context?.let {
            LocalBroadcastManager.getInstance(it)
                .unregisterReceiver(onConnectionLostReceiver)
            LocalBroadcastManager.getInstance(it)
                .unregisterReceiver(onConnectionAvailableReceiver)
        }
    }


    private fun showErrorAlerter(s: String) {
        Toast.makeText(context, s, Toast.LENGTH_LONG).show()
//        val alertDialog = AlertDialog.Builder(context!!) // this: Activity
//            .setMessage(s)
//            .create()
//
//        alertDialog.show()
    }

    private fun showWarningAlerter(s: String) {
        Toast.makeText(context, s, Toast.LENGTH_LONG).show()
    }

    private fun showSuccessAlerter(s: String) {
        Toast.makeText(context, s, Toast.LENGTH_LONG).show()
    }
    protected open fun onNetworkConnectionLost() {
        Log.d(this::javaClass.name,"Connection lost")
    }

    protected open fun onNetworkConnectionAvailable() {
        Log.d(this::javaClass.name,"Connection gained")
    }
}