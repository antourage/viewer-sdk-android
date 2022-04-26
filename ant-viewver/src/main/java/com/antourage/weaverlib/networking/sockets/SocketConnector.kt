package com.antourage.weaverlib.networking.sockets

import android.annotation.SuppressLint
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.antourage.weaverlib.dev_settings.ConfigManager.FEED_URL
import com.antourage.weaverlib.Global
import com.antourage.weaverlib.other.models.PortalStateResponse
import com.antourage.weaverlib.other.models.PortalStateSocketResponse
import com.antourage.weaverlib.ui.AntourageFab
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import com.microsoft.signalr.TransportEnum

internal object SocketConnector {

    lateinit var hubConnection: HubConnection

    var isSocketUsed = true
    private var isConnectTaskRunning = false

    private const val SOCKET_EVENT = "HandleWidgetUpdateEvent"
    const val TAG = "SocketConnector"

    var portalStateLD: MutableLiveData<PortalStateResponse> = MutableLiveData()
    var socketConnection: MutableLiveData<SocketConnection> = MutableLiveData()

    private lateinit var connectToSocketTask: ConnectToSocketTask

    enum class SocketConnection {
        CONNECTED, DISCONNECTED, WAITING
    }

    private var reconnectHandler = Handler(Looper.getMainLooper())
    private const val INITIAL_RECONNECT = -1L
    private const val FIRST_RECONNECT = 0L
    private const val SECOND_RECONNECT = 2000L
    private const val THIRD_RECONNECT = 10000L
    private const val FOURTH_RECONNECT = 30000L
    private const val FIFTH_RECONNECT = 68000L
    private var nextReconnectDelay = INITIAL_RECONNECT
    private var shouldDisconnectSocket = false
    var shouldCallApiRequest = false

    class ConnectToSocketTask : AsyncTask<Long, Any, Any>() {
        @SuppressLint("CheckResult")
        override fun doInBackground(vararg delay: Long?) {
            if (isCancelled) {
                isConnectTaskRunning = false
                return
            }

            if (delay[0] != FIRST_RECONNECT && delay[0] != INITIAL_RECONNECT)
                shouldCallApiRequest = true
            if (delay[0] != INITIAL_RECONNECT) Thread.sleep(delay[0]!!)
            hubConnection.start().blockingGet()
            return
        }

        override fun onPostExecute(result: Any?) {
            super.onPostExecute(result)
            isConnectTaskRunning = false
            if (hubConnection.connectionState == HubConnectionState.DISCONNECTED) {
                Log.d(TAG, "Didn't connect")
            } else {
                Log.d(TAG, "Connected")
                nextReconnectDelay = INITIAL_RECONNECT
                socketConnection.postValue(SocketConnection.CONNECTED)
                isSocketUsed = true
            }
        }
    }

    private fun reconnectWithDelay(delay: Long) {
        Log.d(TAG, "reconnectWithDelay: $delay")
        try {
            connectToSocketTask = ConnectToSocketTask()
            connectToSocketTask.execute(delay)
            isConnectTaskRunning = true
        } catch (e: RuntimeException) {
            e.printStackTrace()
        }
    }

    fun connectToSockets() {
        shouldDisconnectSocket = false
        if (!this::hubConnection.isInitialized) {
            hubConnection = HubConnectionBuilder.create(
                "${FEED_URL}widgethub?teamId=${AntourageFab.teamId}"
            )
                .withTransport(TransportEnum.WEBSOCKETS)
                .shouldSkipNegotiate(true)
                .build()
            initListeners()
        }

        if (hubConnection.connectionState == HubConnectionState.DISCONNECTED) {
            try {
                if (!isConnectTaskRunning) {
                    Log.d(TAG, "Connecting")
                    nextReconnectDelay = INITIAL_RECONNECT
                    connectToSocketTask = ConnectToSocketTask()
                    connectToSocketTask.execute(nextReconnectDelay)
                    isConnectTaskRunning = true
                }
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }
        }
    }

    private fun initListeners() {
        hubConnection.on(SOCKET_EVENT, { response ->
            response?.let {
                response.portalState?.let { portalState ->
                    run {
                        Log.d(TAG, "Portal state received: $it ")
                        handlePortalState(portalState)
                    }
                }
            }
        }, PortalStateSocketResponse::class.java)

        hubConnection.onClosed {
            reconnect()
        }
    }

    private fun handlePortalState(state: PortalStateResponse) {
        portalStateLD.postValue(state)
    }

    private fun reconnect() {
        when (nextReconnectDelay) {
            INITIAL_RECONNECT -> {
                nextReconnectDelay = FIRST_RECONNECT
            }
            FIRST_RECONNECT -> {
                nextReconnectDelay = SECOND_RECONNECT
            }
            SECOND_RECONNECT -> {
                nextReconnectDelay = THIRD_RECONNECT
            }
            THIRD_RECONNECT -> {
                nextReconnectDelay = FOURTH_RECONNECT
            }
            FOURTH_RECONNECT -> {
                nextReconnectDelay = FIFTH_RECONNECT
            }
            FIFTH_RECONNECT -> {
                nextReconnectDelay = INITIAL_RECONNECT
                if (Global.networkAvailable) {
                    isSocketUsed = false
                    shouldDisconnectSocket = true
                    socketConnection.postValue(SocketConnection.DISCONNECTED)
                }
            }
        }

        if (!shouldDisconnectSocket) {
            if (Global.networkAvailable) {
                Log.d(TAG, "Starting reconnect")
                reconnectWithDelay(nextReconnectDelay)
                isConnectTaskRunning = true
            }
        } else {
            Log.d(TAG, "Disconnected")
        }
    }

    private fun clearSocketData() {
        portalStateLD.postValue(null)
    }

    fun disconnectSocket() {
        isConnectTaskRunning = false
        shouldCallApiRequest = false
        shouldDisconnectSocket = true
        socketConnection.postValue(SocketConnection.WAITING)
        reconnectHandler.removeCallbacksAndMessages(null)
        clearSocketData()
        try {
            if (this::hubConnection.isInitialized) {
                if (hubConnection.connectionState == HubConnectionState.CONNECTED) {
                    hubConnection.stop()
                }
            }
            if (this::connectToSocketTask.isInitialized) {
                connectToSocketTask.cancel(true)
            }
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

}