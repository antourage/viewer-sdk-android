package com.antourage.weaverlib.other.networking

import android.annotation.SuppressLint
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.antourage.weaverlib.Global
import com.antourage.weaverlib.other.models.ListOfStreams
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.screens.base.Repository
import com.antourage.weaverlib.ui.fab.AntourageFab
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import java.lang.RuntimeException
import java.util.*
import kotlin.collections.ArrayList

internal object SocketConnector {

    lateinit var hubConnection: HubConnection

    var isSocketUsed = true

    const val SOCKET_LIVE = "LiveStreams"
    const val SOCKET_VOD = "NewVod"
    const val SOCKET_VIEWS = "LiveViewerCount"

    var newLivesLiveData: MutableLiveData<List<StreamResponse>> = MutableLiveData()
    var newVodsLiveData: MutableLiveData<List<StreamResponse>> = MutableLiveData()
    var socketConnection: MutableLiveData<SocketConnection> = MutableLiveData()

    private lateinit var connectToSocketTask: ConnectToSocketTask
    private lateinit var reconnectToSocketTask: ReconnectToSocketTask

    enum class SocketConnection {
        CONNECTED, DISCONNECTED, WAITING
    }

    private var reconnectHandler = Handler(Looper.getMainLooper())
    private const val FIRST_RECONNECT = 0L
    private const val SECOND_RECONNECT = 1000L
    private const val THIRD_RECONNECT = 2000L
    private const val FOURTH_RECONNECT = 4000L
    private var shouldDisconnectSocket = false


    class ReconnectToSocketTask : AsyncTask<Long, Any, Long>() {
        override fun doInBackground(vararg p0: Long?): Long? {
            if(isCancelled) return -1L
            hubConnection.start().blockingGet()
            return p0[0]!!
        }

        override fun onPostExecute(result: Long?) {
            super.onPostExecute(result)
            if(result!! == -1L) return
            if (hubConnection.connectionState == HubConnectionState.DISCONNECTED) {
                when (result!!) {
                    FIRST_RECONNECT -> {
                        reconnectWithDelay(SECOND_RECONNECT)
                    }
                    SECOND_RECONNECT -> {
                        reconnectWithDelay(THIRD_RECONNECT)
                    }
                    THIRD_RECONNECT -> {
                        reconnectWithDelay(FOURTH_RECONNECT)
                    }
                    FOURTH_RECONNECT -> {
                        if (Global.networkAvailable) {
                            isSocketUsed = false
                            socketConnection.postValue(SocketConnection.DISCONNECTED)
                        }
                    }
                }
            } else {
                isSocketUsed = true
                socketConnection.postValue(SocketConnection.CONNECTED)
            }
        }
    }

    fun reconnectWithDelay(delay: Long) {
        reconnectHandler.postDelayed({
            try {
                reconnectToSocketTask = ReconnectToSocketTask()
                reconnectToSocketTask.execute(delay)
            }catch (e: RuntimeException){
                e.printStackTrace()
            }
        }, delay)
    }

    class ConnectToSocketTask : AsyncTask<Any, Any, Any>() {
        @SuppressLint("CheckResult")
        override fun doInBackground(vararg params: Any?) {
            if(isCancelled) return
            hubConnection.start().blockingGet()
            return
        }

        override fun onPostExecute(result: Any?) {
            super.onPostExecute(result)
            if (hubConnection.connectionState == HubConnectionState.DISCONNECTED) {
                reconnectHandler.postDelayed({
                    reconnectToSocketTask = ReconnectToSocketTask()
                    reconnectToSocketTask.execute(FIRST_RECONNECT)
                }, FIRST_RECONNECT)
            } else {
                socketConnection.postValue(SocketConnection.CONNECTED)
                isSocketUsed = true
            }
        }
    }

    fun isConnected(): Boolean {
        return this::hubConnection.isInitialized && hubConnection.connectionState == HubConnectionState.CONNECTED
    }

    fun connectToSockets(token: String) {
        shouldDisconnectSocket = false
        if(!this::hubConnection.isInitialized || hubConnection.connectionState == HubConnectionState.DISCONNECTED) hubConnection = HubConnectionBuilder.create(
            "${ApiClient.BASE_URL}hub?access_token=${token}"
        )
            .shouldSkipNegotiate(true)
            .build()

        if (hubConnection.connectionState == HubConnectionState.DISCONNECTED) {
            try {
                connectToSocketTask = ConnectToSocketTask()
                connectToSocketTask.execute()

                hubConnection.on(SOCKET_LIVE, { newStreams ->
                    if (newStreams != null) {
                        val newLives = newStreams as ArrayList<StreamResponse>
                        newLivesLiveData.postValue(newLives)
                        Log.e(AntourageFab.TAG, "sockets live: $newStreams")
                    }
                }, ListOfStreams::class.java)

                hubConnection.on(SOCKET_VOD, { newStreams ->
                    val newVods = newStreams as ArrayList<StreamResponse>
                    newVodsLiveData.postValue(newVods)
                    Log.e(AntourageFab.TAG, "sockets vod: $newStreams")
                }, ListOfStreams::class.java)

                hubConnection.on(SOCKET_VIEWS, {
                    Log.e(AntourageFab.TAG, "sockets views")
                }, Objects::class.java)

                hubConnection.onClosed {
                    if (!shouldDisconnectSocket) {
                        reconnectToSocketTask = ReconnectToSocketTask()
                        reconnectToSocketTask.execute(FIRST_RECONNECT)
                        Log.e(AntourageFab.TAG, "socket closed - start reconnect")
                    } else {
                        Log.e(AntourageFab.TAG, "socket closed - end")
                    }
                }
            }catch (e: RuntimeException){
                e.printStackTrace()
            }
        }
    }

    fun cancelReconnect() {
        if(this::connectToSocketTask.isInitialized) connectToSocketTask.cancel(true)
        if(this::reconnectToSocketTask.isInitialized) reconnectToSocketTask.cancel(true)
        socketConnection.postValue(SocketConnection.WAITING)
        reconnectHandler.removeCallbacksAndMessages(null)
    }

    fun disconnectSocket() {
        shouldDisconnectSocket = true
        socketConnection.postValue(SocketConnection.WAITING)
        reconnectHandler.removeCallbacksAndMessages(null)
        if (this::hubConnection.isInitialized) {
            hubConnection.stop()
//            hubConnection.close()
        }
    }

}