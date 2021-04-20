@file:Suppress("CAST_NEVER_SUCCEEDS")

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
import com.antourage.weaverlib.other.models.LiveUpdatedResponse
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.auth.AuthClient
import com.antourage.weaverlib.screens.list.ReceivingVideosManager
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import com.microsoft.signalr.TransportEnum

internal object SocketConnector {

    lateinit var hubConnection: HubConnection

    var isSocketUsed = true
    private var isConnectTaskRunning = false

    private const val SOCKET_LIVE = "LiveStreams"
    private const val SOCKET_LIVE_STARTED = "LiveStreamStarted"
    private const val SOCKET_LIVE_UPDATED = "LiveStreamUpdated"
    private const val SOCKET_LIVE_FINISHED = "LiveStreamFinished"
    private const val SOCKET_VOD = "NewFeedItem"
    const val TAG = "SocketConnector"

    var newLivesLiveData: MutableLiveData<List<StreamResponse>> = MutableLiveData()
    var newVodLiveData: MutableLiveData<StreamResponse> = MutableLiveData()
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
        hubConnection.on(SOCKET_LIVE, {
            /** added this as old event SOCKET_LIVE still works via backwards compatibility on old versions
             * without this empty handler there will be hufe error in logs from SignalR lib
             */
        }, Object::class.java)

        hubConnection.on(SOCKET_LIVE_STARTED, { newStream ->
            newStream?.let {
                handleNewLiveStarted(it)
                Log.d(TAG, "live started: $it ")
            }
        }, StreamResponse::class.java)
//
        hubConnection.on(SOCKET_LIVE_FINISHED, { id ->
            id?.let {
                handleLiveFinished(it as Int)
                Log.d(TAG, "live finished: $it ")
            }
        }, Integer::class.java)
//
        hubConnection.on(SOCKET_LIVE_UPDATED, { response ->
            response?.let {
                handleLiveUpdated(response)
                Log.d(TAG, "live updated: id ${response.id} viewers ${response.viewerCount}")
            }
        }, LiveUpdatedResponse::class.java)


        hubConnection.on(SOCKET_VOD, { newVod ->
            newVod?.let {
                if (newVod.isNotEmpty()) newVodLiveData.postValue(it[0])
            }
            Log.d(TAG, "vod received:  $newVod")
        }, ListOfStreams::class.java)

        hubConnection.onClosed { exception ->
            if (exception != null && exception.message?.contains("401")!!) {
                Log.d(TAG, "Socket token expired")
                if (Global.networkAvailable) {
                    synchronized(ApiClient.getHttpClient()) {
                        UserCache.getInstance()?.getIdToken()?.let {
                            if (it == currentToken) {
                                isConnectTaskRunning = false
                                val code: Int =
                                    AuthClient.getAuthClient().authenticateUser().code() / 100
                                if (code == 2) {
                                    if (UserCache.getInstance()?.getIdToken() != null) {
                                        connectToSockets(UserCache.getInstance()?.getIdToken()!!)
                                    }
                                }
                            } else {
                                reconnect()
                            }
                        }
                    }
                }
            } else {
                reconnect()
            }
        }
    }

    private fun handleNewLiveStarted(newLive: StreamResponse) {
        val livesList = ReceivingVideosManager.liveVideos
        if (!livesList.any { it.id == newLive.id }) {
            livesList.add(0, newLive)
            newLivesLiveData.postValue(livesList)
        }
    }

    private fun handleLiveFinished(id: Int) {
        val livesList = ReceivingVideosManager.liveVideos
        livesList.removeAll { it.id == id }
        newLivesLiveData.postValue(livesList)
    }

    private fun handleLiveUpdated(liveUpdatedResponse: LiveUpdatedResponse) {
        val livesList = ReceivingVideosManager.liveVideos
        livesList.filter { it.id == liveUpdatedResponse.id }.forEach {
            it.viewersCount = liveUpdatedResponse.viewerCount
        }
        newLivesLiveData.postValue(livesList)
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
        newVodLiveData.value = null
        try {
            if (this::hubConnection.isInitialized) {
                if (hubConnection.connectionState == HubConnectionState.CONNECTED) {
                    hubConnection.stop()
                }
            }
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

}