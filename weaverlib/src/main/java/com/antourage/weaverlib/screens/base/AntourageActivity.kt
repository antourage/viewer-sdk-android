package com.antourage.weaverlib.screens.base

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.NetworkStateReceiver
import com.antourage.weaverlib.screens.list.VideoListFragment
import com.antourage.weaverlib.screens.weaver.WeaverFragment
import com.antourage.weaverlib.ui.fab.AntourageFab.Companion.ARGS_STREAM_SELECTED
import com.google.firebase.FirebaseApp


class AntourageActivity : AppCompatActivity(), NetworkStateReceiver.NetworkStateReceiverListener  {


    companion object {
        const val ACTION_CONNECTION_LOST = "action_internet_connection_lost"
        const val ACTION_CONNECTION_AVAILABLE = "action_internet_connection_available"
        fun initAntourage(context: Context){
            FirebaseApp.initializeApp(context)
        }
    }

    private lateinit var networkStateReceiver: NetworkStateReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_antourage)
        val inteee = intent
        val extra =  intent.getParcelableExtra<StreamResponse>(ARGS_STREAM_SELECTED)
        if(intent?.extras?.getParcelable<StreamResponse>(ARGS_STREAM_SELECTED) != null){
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainContent, WeaverFragment.newInstance(intent.getParcelableExtra(ARGS_STREAM_SELECTED))).commit()
        } else
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainContent, VideoListFragment.newInstance()).commit()
        FirebaseLoginService(this).handleSignIn()
        networkStateReceiver = NetworkStateReceiver()
        networkStateReceiver.addListener(this)
        this.registerReceiver(networkStateReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun networkAvailable() {
        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        val intent = Intent(ACTION_CONNECTION_AVAILABLE)
        localBroadcastManager.sendBroadcast(intent)
    }

    override fun networkUnavailable() {
        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        val intent = Intent(ACTION_CONNECTION_LOST)
        localBroadcastManager.sendBroadcast(intent)
    }
    override fun onUserLeaveHint() {
        //TODO uncomment and enable
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
//            && supportFragmentManager.findFragmentById(R.id.mainContent) is StreamingFragment<*>
//        ) {
//            enterPictureInPictureMode(
//                with(PictureInPictureParams.Builder()) {
//                    val width = 16
//                    val height = 9
//                    setAspectRatio(Rational(width, height))
//                    build()
//                })
//        }
    }

    //region Fullscreen leanback mode
//    override fun onWindowFocusChanged(hasFocus: Boolean) {
//        super.onWindowFocusChanged(hasFocus)
//        val orientation = resources.configuration.orientation
//        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            if (hasFocus) hideSystemUI()
//        }
//    }
//
//    public fun hideSystemUI() {
//        window?.decorView?.systemUiVisibility = (
//                // Set the content to appear under the system bars so that the
//                // content doesn't resize when the system bars hide and show.
//                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                        // Hide the nav bar and status bar
//                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                        or View.SYSTEM_UI_FLAG_FULLSCREEN)
//    }
//
//    // Shows the system bars by removing all the flags
//    // except for the ones that make the content appear under the system bars.
//    private fun showSystemUI() {
//        window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
//    }
    //endregion

}
