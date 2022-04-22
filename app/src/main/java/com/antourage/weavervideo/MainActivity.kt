package com.antourage.weavervideo

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.multidex.MultiDex
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.antourage.weaverlib.ui.fab.AntourageFab
import com.antourage.weaverlib.ui.fab.RegisterPushNotificationsResult
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private var prefs: SharedPreferences? = null
    private lateinit var connectivityManager: ConnectivityManager

    companion object {
        const val LANGUAGE = "language"
        const val PREFS = "test_prefs"
        const val TAG = "Antourage_testing_tag"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MultiDex.install(this)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        LocaleHelper.setLocale(this, getLanguage())

        Picasso.get().load(R.drawable.hacken_header).into(header)
        Picasso.get().load(R.drawable.hacken_header_overlay).into(header_overlay)

        val bottomNavigationView = findViewById<BottomNavigationView
                >(R.id.bottom_navigation_view)
        val navController = findNavController(R.id.nav_fragment)
        bottomNavigationView.setupWithNavController(
            navController
        )

        connectivityManager =
            this@MainActivity.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager

        //region Antourage configuration
        AntourageFab.configure(this, 1)
        //endregion

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
        }.start()
        //endregion
    }

    fun getLanguage(): String {
        var language = prefs?.getString(LANGUAGE, null)
        if (language.isNullOrEmpty()) {
            language = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                resources.configuration.locales[0].toLanguageTag().split("-")
                    .first()
            } else {
                resources.configuration.locale.toLanguageTag().split("-").first()
            }
        }

        return language
    }

    fun updateLanguageChoice(language: String?) {
        if(language == null){
            prefs?.edit()
                ?.remove(LANGUAGE)
                ?.apply()
        }else{
            prefs?.edit()
                ?.putString(LANGUAGE, language.lowercase())
                ?.apply()
        }
    }
}
