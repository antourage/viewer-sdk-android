package com.antourage.weavervideo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.multidex.MultiDex
import com.antourage.weaverlib.ui.fab.AntourageFab
import com.antourage.weaverlib.ui.fab.RegisterPushNotificationsResult
import com.antourage.weaverlib.ui.fab.UserAuthResult
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "Antourage_testing_tag"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MultiDex.install(this)
        setContentView(R.layout.activity_main)

        //region Antourage authorization
        antfab.authWith(TEST_API_KEY.toUpperCase(), callback = { userAuthResult ->
            when (userAuthResult) {
                is UserAuthResult.Success -> {
                    Log.d(TAG, "Ant authorization successful!")

                    //region Antourage push notification subscription
                    Thread(Runnable {
                        try {
                            //Get firebase cloud messaging token
                            val fcmToken =
                                FirebaseInstanceId.getInstance().getToken(getString(R.string.SENDER_ID), "FCM")
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
                                                Log.d(TAG, "Subscription failed: ${subscriptionResult.cause}")
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }).start()
                    //endregion

                }
                is UserAuthResult.Failure -> {
                    Log.e(TAG, "Ant authorization failed because: ${userAuthResult.cause}")
                }
            }
        })
        //endregion
    }

    override fun onResume() {
        super.onResume()
        antfab.onResume()
    }

    override fun onPause() {
        super.onPause()
        antfab.onPause()
    }
}
