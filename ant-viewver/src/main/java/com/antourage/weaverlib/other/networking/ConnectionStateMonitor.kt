package com.antourage.weaverlib.other.networking

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import androidx.lifecycle.MutableLiveData
import com.antourage.weaverlib.Global


internal class ConnectionStateMonitor(val context: Context) :
    ConnectivityManager.NetworkCallback() {

    companion object {
        fun isNetworkAvailable(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val networkCapabilities = connectivityManager.activeNetwork ?: return false
                val activeNetwork =
                    connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false

                return when {

                    activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    else -> false
                }
            } else {
                return connectivityManager.activeNetworkInfo != null &&
                        connectivityManager.activeNetworkInfo!!.isConnectedOrConnecting
            }
        }

        val internetStateLiveData = MutableLiveData<NetworkConnectionState>()
    }

    private val handler: Handler = Handler()

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
        handler.removeCallbacks(finalNetworkCheck);
        if (internetStateLiveData.value != NetworkConnectionState.AVAILABLE)
            internetStateLiveData.postValue(NetworkConnectionState.AVAILABLE)
        Global.networkAvailable = true
        Handler().postDelayed({ internetStateLiveData.postValue(null) }, 500)
    }

    /** means that no network is available for sure */
    private val finalNetworkCheck = Runnable {
        Global.networkAvailable = isNetworkAvailable(context)
        internetStateLiveData.postValue(NetworkConnectionState.LOST)
        /**
         * Need this post(null) in order to ignore cached live data value and
         * show alerter only in case when the value is set and not when the
         * live data is subscribed;
         * Without this thing, alerter could be shown twice in a row sometimes;
         */
        Handler().postDelayed({ internetStateLiveData.postValue(null) }, 500)
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        handler.postDelayed(finalNetworkCheck, 1000);
    }
}