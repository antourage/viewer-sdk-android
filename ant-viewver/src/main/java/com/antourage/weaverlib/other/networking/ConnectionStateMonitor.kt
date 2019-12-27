package com.antourage.weaverlib.other.networking

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.net.*
import com.antourage.weaverlib.Global

internal class ConnectionStateMonitor(val context: Context) :
    ConnectivityManager.NetworkCallback() {

    companion object {
        fun isNetworkAvailable(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }

        val internetStateLiveData = MutableLiveData<NetworkConnectionState>()
    }

    private val networkRequest: NetworkRequest =
        NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()

    init {
        Global.networkAvailable = isNetworkAvailable(context)
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerNetworkCallback(networkRequest, this)
    }

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        if (internetStateLiveData.value != NetworkConnectionState.AVAILABLE)
            internetStateLiveData.postValue(NetworkConnectionState.AVAILABLE)
        Global.networkAvailable = true
        android.os.Handler().postDelayed({ internetStateLiveData.postValue(null) }, 500)
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        Global.networkAvailable = activeNetwork?.isConnected ?: false
        internetStateLiveData.postValue(NetworkConnectionState.LOST)
        /**
         * Need this post(null) in order to ignore cached live data value and
         * show alerter only in case when the value is set and not when the
         * live data is subscribed;
         * Without this thing, alerter could be shown twice in a row sometimes;
         */
        android.os.Handler().postDelayed({ internetStateLiveData.postValue(null) }, 500)
    }
}