package com.antourage.weavervideo

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.multidex.MultiDex
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog
import com.antourage.weaverlib.screens.list.dev_settings.OnDevSettingsChangedListener
import com.antourage.weaverlib.ui.fab.AntourageFab
import com.antourage.weaverlib.ui.fab.RegisterPushNotificationsResult
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException


class MainActivity : AppCompatActivity() , OnDevSettingsChangedListener{

    private lateinit var connectivityManager: ConnectivityManager

    companion object {
        const val TAG = "Antourage_testing_tag"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MultiDex.install(this)
        setContentView(R.layout.activity_main)

        Picasso.get().load(R.drawable.hacken_header).into(header)
        Picasso.get().load(R.drawable.hacken_header_overlay).into(header_overlay)
        Picasso.get().load(R.drawable.hacken_footer).into(footer)
        Picasso.get().load(R.drawable.hacken_background).into(main_content)

        connectivityManager =
            this@MainActivity.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager

        //region Antourage configuration
        AntourageFab.configure(this,1)
        //endregion

        antfab.setLifecycle(lifecycle)

        header.setOnClickListener {
            val dialog = DevSettingsDialog(this@MainActivity, this)
            dialog.show()
        }

        //region Antourage push notification subscription
        var fcmToken: String? = ""
        Thread {
            try {
                fcmToken =
                    FirebaseInstanceId.getInstance().getToken(AntourageFab.AntourageSenderId, "FCM")
            } catch (e: IOException) {
                e.printStackTrace()
            }
            runOnUiThread {
                AntourageFab.registerNotifications(fcmToken, 1 ) { subscriptionResult ->
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
        }.start()
        //endregion
    }

    override fun onBeChanged(choice: String) {
        choice.let {
            UserCache.getInstance(application.applicationContext)
                ?.updateEnvChoice(choice)
            UserCache.getInstance()?.saveAccessToken(null)
            UserCache.getInstance()?.saveIdToken(null)
            UserCache.getInstance()?.saveRefreshToken(null)
        }
    }
}
