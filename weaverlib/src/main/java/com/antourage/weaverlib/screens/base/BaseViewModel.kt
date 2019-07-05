package com.antourage.weaverlib.screens.base

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData

open class BaseViewModel constructor(application: Application) : AndroidViewModel(application) {
    companion object {
        var error: MutableLiveData<String?> = MutableLiveData()
        var warning: MutableLiveData<String?> = MutableLiveData()
        var success: MutableLiveData<String?> = MutableLiveData()
    }

}