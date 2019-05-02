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
        BaseViewModel.error.removeObserver(errorObserver)
        BaseViewModel.error.observe(this, errorObserver)

        BaseViewModel.warning.removeObserver(warningObserver)
        BaseViewModel.warning.observe(this, warningObserver)

        BaseViewModel.success.removeObserver(successObserver)
        BaseViewModel.success.observe(this, successObserver)

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

    protected fun replaceFragment(
        fragment: BaseFragment<*>,
        addToBackStack: Boolean
    ) {
        replaceFragment(
            fragment, addToBackStack,
            FragmentTransaction.TRANSIT_NONE, FragmentTransaction.TRANSIT_NONE,
            FragmentTransaction.TRANSIT_NONE, FragmentTransaction.TRANSIT_NONE
        )
    }
    protected fun replaceFragment(
        fragment: BaseFragment<*>,
        addToBackStack: Boolean,
        @AnimRes enterAnimId: Int,
        @AnimRes exitAnimId: Int,
        @AnimRes popEnterAnimId: Int,
        @AnimRes popExitAnimId: Int
    ) {
        try {
            if (activity != null) {
                val transaction = activity!!.supportFragmentManager.beginTransaction()
                transaction.setCustomAnimations(enterAnimId, exitAnimId, popEnterAnimId, popExitAnimId)
                transaction.replace(R.id.mainContent, fragment, fragment.javaClass.simpleName)
                if (addToBackStack) {
                    transaction.addToBackStack(fragment.javaClass.simpleName)
                }
                transaction.commit()
            }
        } catch (e: IllegalStateException) {
            Log.d("ERROR", "error")
        }

    }


}