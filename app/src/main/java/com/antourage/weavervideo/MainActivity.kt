package com.antourage.weavervideo

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.multidex.MultiDex
import com.antourage.weaverlib.ui.fab.AntourageFab
import com.antourage.weaverlib.ui.fab.RegisterPushNotificationsResult
import com.antourage.weaverlib.ui.fab.UserAuthResult
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException


class MainActivity : AppCompatActivity() {

    //        lateinit var antfab: AntourageFab
    private var isUserAuthorized = false
    private lateinit var connectivityManager: ConnectivityManager

    companion object {
        const val TAG = "Antourage_testing_tag"
        const val TEST_API_KEY = "A5F76EE9-BC76-4F76-A042-933B8993FC2C"
//        const val TEST_API_KEY = "49D7E915-549B-4B79-9D61-FF5E5C85D2C2"
//        const val TEST_API_KEY = "4ec7cb01-a379-4362-a3a4-89699c17dc32"
//        const val TEST_API_KEY = "472EC909-BB20-4B86-A192-3A78C35DD3BA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MultiDex.install(this)
        setContentView(R.layout.activity_main)

        Picasso.get().load(R.drawable.hacken_header).into(header)
        Picasso.get().load(R.drawable.hacken_header_overlay).into(header_overlay)
        Picasso.get().load(R.drawable.hacken_footer).into(footer)
        Picasso.get().load(R.drawable.hacken_background).into(mainContent)

        connectivityManager =
            this@MainActivity.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager


//        /** To add widget programatically*/
//        antfab = AntourageFab(this)
//        if(antfab.parent == null){
//            antfab.setPosition("bottomRight")
//            antfab.showFab(this)
//            antfab.setLocale("sv")
//            antfab.setMargins(0, 0)
//        }

        authWidget()
    }

    private fun authWidget() {
        //region Antourage authorization
        antfab.authWith(TEST_API_KEY.toUpperCase())

        //region Antourage push notification subscription
        Thread {
            try {
                //Get firebase cloud messaging token
                val fcmToken =
                    FirebaseInstanceId.getInstance()
                        .getToken(getString(R.string.SENDER_ID), "FCM")
                runOnUiThread {
                    fcmToken?.let { fcmToken ->
                        AntourageFab.registerNotifications(fcmToken) { subscriptionResult ->
                            //Handle subscription result
                            when (subscriptionResult) {
                                //If result is successful, subscribe to the topic with
                                //topic name from result.
                                is RegisterPushNotificationsResult.Success -> {
                                    Log.d(
                                        TAG,
                                        "Subscribed successfully; Topic name= ${subscriptionResult.topicName}"
                                    )
                                    FirebaseMessaging.getInstance()
                                        .subscribeToTopic(subscriptionResult.topicName)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                Log.d(TAG, "Subscribed successfully!")
                                            } else {
                                                Log.d(TAG, "Subscription failed(")
                                            }
                                        }
                                }
                                is RegisterPushNotificationsResult.Failure -> {
                                    Log.d(
                                        TAG,
                                        "Subscription failed: ${subscriptionResult.cause}"
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
        //endregion
    }
    //endregion

//    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
//        override fun onAvailable(network: Network?) {
//            runOnUiThread {
//                if (!isUserAuthorized) authWidget()
//            }
//        }
//
//        override fun onLost(network: Network?) {
//        }
//    }

//    private fun subscribeToNetworkChanges() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            connectivityManager.registerDefaultNetworkCallback(networkCallback)
//        } else {
//            val request = NetworkRequest.Builder()
//                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
//            connectivityManager.registerNetworkCallback(request, networkCallback)
//        }
//    }

    override fun onResume() {
        super.onResume()
        antfab.onResume()
//        subscribeToNetworkChanges()
    }

    override fun onPause() {
        super.onPause()
        antfab.onPause()
//        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}
