package com.antourage.weaverlib.other.networking

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.net.*
import com.antourage.weaverlib.Global

internal class ConnectionStateMonitor(val context: Context) : ConnectivityManager.NetworkCallback() {

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
        internetStateLiveData.postValue(NetworkConnectionState.AVAILABLE)
        Global.networkAvailable = true
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        internetStateLiveData.postValue(NetworkConnectionState.LOST)
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        Global.networkAvailable = activeNetwork?.isConnected ?: false
    }
}