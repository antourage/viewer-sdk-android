package com.antourage.weaverlib.other.networking

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.antourage.weaverlib.Global

internal class ConnectionStateMonitor(val context: Context) :
    ConnectivityManager.NetworkCallback() {

    init {
        initManager(context)
        Global.networkAvailable = isNetworkAvailable()
    }

    companion object {
        private lateinit var connectivityManager: ConnectivityManager
        private val handler: Handler = Handler()
        val internetStateLiveData = MutableLiveData<NetworkConnectionState>()
        private val networkRequest: NetworkRequest =
            NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()

        fun isNetworkAvailable(): Boolean {
            var result = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val networkCapabilities = connectivityManager.activeNetwork ?: return false
                val actNw =
                    connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
                result = when {
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    else -> false
                }
            } else {
                connectivityManager.activeNetworkInfo?.run {
                    result = when (type) {
                        ConnectivityManager.TYPE_WIFI -> true
                        ConnectivityManager.TYPE_MOBILE -> true
                        ConnectivityManager.TYPE_ETHERNET -> true
                        else -> false
                    }
                }
            }
            return result
        }

        private fun initManager(context: Context) {
            connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.registerNetworkCallback(networkRequest, object :
                ConnectivityManager.NetworkCallback() {

                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    handler.removeCallbacks(finalNetworkCheck)
                    if (isNetworkAvailable()) {
                        Global.networkAvailable = true
                        internetStateLiveData.postValue(NetworkConnectionState.AVAILABLE)
                    } else {
                        Global.networkAvailable = false
                    }
                    Handler().postDelayed({ internetStateLiveData.postValue(null) }, 500)
                }

                /** means that no network is available for sure */
                private val finalNetworkCheck = Runnable {
                    if (!isNetworkAvailable()) {
                        Global.networkAvailable = false
                        internetStateLiveData.postValue(NetworkConnectionState.LOST)
                        /**
                         * Need this post(null) in order to ignore cached live data value and
                         * show alerter only in case when the value is set and not when the
                         * live data is subscribed;
                         * Without this thing, alerter could be shown twice in a row sometimes;
                         */
                        Handler().postDelayed({ internetStateLiveData.postValue(null) }, 500)
                    } else {
                        Global.networkAvailable = true
                        internetStateLiveData.postValue(NetworkConnectionState.AVAILABLE)
                        Handler().postDelayed({ internetStateLiveData.postValue(null) }, 500)
                    }
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    if (!isNetworkAvailable()) {
                        Global.networkAvailable = false
                        internetStateLiveData.postValue(NetworkConnectionState.LOST)
                        Handler().postDelayed({ internetStateLiveData.postValue(null) }, 500)
                        handler.postDelayed(finalNetworkCheck, 1000);
                    }
                }
            })
        }
    }
}