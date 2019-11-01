package com.antourage.weavervideo

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.multidex.MultiDex
import com.antourage.weaverlib.ui.fab.AntourageFab
import com.antourage.weaverlib.ui.fab.UserAuthResult
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    companion object {
        //TODO: delete
        const val API_KEY_1 = "a5f76ee9-bc76-4f76-a042-933b8993fc2c"
        const val API_KEY_2 = "4ec7cb01-a379-4362-a3a4-89699c17dc32"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MultiDex.install(this)
        setContentView(R.layout.activity_main)
        antfab.authWith(API_KEY_2, callback = {
            when (it) {
                is UserAuthResult.Success -> {
                }
                is UserAuthResult.Failure -> {
                }
            }
        })

        Thread(Runnable {
            try {
                val fcmToken =
                    FirebaseInstanceId.getInstance().getToken(getString(R.string.SENDER_ID), "FCM")
                runOnUiThread { fcmToken?.let { AntourageFab.registerNotifications(it) } }
                FirebaseMessaging.getInstance().subscribeToTopic("ssl-leo-antourage-a")
                    .addOnCompleteListener { task ->
                        var msg = getString(R.string.msg_subscribed)
                        if (!task.isSuccessful) {
                            msg = getString(R.string.msg_subscribe_failed)
                        }
//                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }).start()
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
