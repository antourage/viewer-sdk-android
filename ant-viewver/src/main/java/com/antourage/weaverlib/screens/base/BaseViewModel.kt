package com.antourage.weaverlib.screens.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

internal open class BaseViewModel constructor(application: Application) : AndroidViewModel(application) {
    companion object {
        var error: MutableLiveData<String?> = MutableLiveData()
        var warning: MutableLiveData<String?> = MutableLiveData()
        var success: MutableLiveData<String?> = MutableLiveData()
    }
}