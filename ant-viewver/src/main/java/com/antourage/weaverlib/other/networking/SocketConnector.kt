package com.antourage.weaverlib.other.networking

import android.annotation.SuppressLint
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.antourage.weaverlib.Global
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.models.ListOfStreams
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.auth.AuthClient
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import com.microsoft.signalr.TransportEnum

internal object SocketConnector {

    lateinit var hubConnection: HubConnection

    var isSocketUsed = true
    private var isConnectTaskRunning = false

    const val SOCKET_LIVE = "LiveStreams"
    const val SOCKET_VOD = "NewVod"
    const val TAG = "SocketConnector"

    var newLivesLiveData: MutableLiveData<List<StreamResponse>> = MutableLiveData()
    var newVodsLiveData: MutableLiveData<List<StreamResponse>> = MutableLiveData()
    var socketConnection: MutableLiveData<SocketConnection> = MutableLiveData()

    private lateinit var connectToSocketTask: ConnectToSocketTask

    enum class SocketConnection {
        CONNECTED, DISCONNECTED, WAITING
    }

    private var reconnectHandler = Handler(Looper.getMainLooper())
    private const val INITIAL_RECONNECT = -1L
    private const val FIRST_RECONNECT = 0L
    private const val SECOND_RECONNECT = 1000L
    private const val THIRD_RECONNECT = 2000L
    private const val FOURTH_RECONNECT = 4000L
    private var nextReconnectDelay = INITIAL_RECONNECT
    private var shouldDisconnectSocket = false
    private var currentToken = ""

    class ConnectToSocketTask : AsyncTask<Long, Any, Any>() {
        @SuppressLint("CheckResult")
        override fun doInBackground(vararg delay: Long?) {
            if (isCancelled) {
                isConnectTaskRunning = false
                return
            }

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

    fun isConnected(): Boolean {
        return this::hubConnection.isInitialized && hubConnection.connectionState == HubConnectionState.CONNECTED
    }

    fun connectToSockets(token: String) {
        shouldDisconnectSocket = false
        if (!this::hubConnection.isInitialized || currentToken != token) {
            currentToken = token
            hubConnection = HubConnectionBuilder.create(
                "${ApiClient.BASE_URL}hub?access_token=${token}"
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
        hubConnection.on(SOCKET_LIVE, { newStreams ->
            if (newStreams != null) {
                val newLives = newStreams as ArrayList<StreamResponse>
                newLivesLiveData.postValue(newLives)
                Log.d(TAG, "live: $newStreams ")
            }
        }, ListOfStreams::class.java)

        hubConnection.on(SOCKET_VOD, { newStreams ->
            val newVods = newStreams as ArrayList<StreamResponse>
            newVodsLiveData.postValue(newVods)
            Log.d(TAG, "vod:  $newStreams")
        }, ListOfStreams::class.java)

        hubConnection.onClosed { exception ->
            if(exception!=null && exception.message?.contains("401")!!){
                Log.d(TAG, "Socket token expired")
                if (Global.networkAvailable) {
                    synchronized(ApiClient.getHttpClient()) {
                        UserCache.getInstance()?.getAccessToken()?.let {
                            if (it == currentToken) {
                                isConnectTaskRunning = false
                                val code: Int =
                                    AuthClient.getAuthClient().authenticateUser().code() / 100
                                if (code == 2) {
                                    if (UserCache.getInstance()?.getAccessToken() != null) {
                                        connectToSockets(UserCache.getInstance()?.getAccessToken()!!)
                                    }
                                }
                            } else {
                                reconnect()
                            }
                        }
                    }
                }
            }else{
                reconnect()
            }
        }
    }

    private fun reconnect(){
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

    fun cancelReconnect() {
        if (this::connectToSocketTask.isInitialized) connectToSocketTask.cancel(true)
        isConnectTaskRunning = false
        socketConnection.postValue(SocketConnection.WAITING)
        reconnectHandler.removeCallbacksAndMessages(null)
    }

    fun disconnectSocket() {
        shouldDisconnectSocket = true
        socketConnection.postValue(SocketConnection.WAITING)
        reconnectHandler.removeCallbacksAndMessages(null)
        newLivesLiveData.value = null
        newVodsLiveData.value = null
        try{
            if (this::hubConnection.isInitialized) {
                if (hubConnection.connectionState == HubConnectionState.CONNECTED) {
                    hubConnection.stop()
                }
            }
        }catch (e: NullPointerException){
            e.printStackTrace()
        }
    }

}