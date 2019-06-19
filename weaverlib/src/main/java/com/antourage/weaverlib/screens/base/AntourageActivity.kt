package com.antourage.weaverlib.screens.base

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.ApiClient.BASE_URL
import com.antourage.weaverlib.other.networking.NetworkStateReceiver
import com.antourage.weaverlib.other.ui.keyboard.KeyboardEventListener
import com.antourage.weaverlib.screens.list.VideoListFragment
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog
import com.antourage.weaverlib.screens.weaver.WeaverFragment
import com.antourage.weaverlib.ui.fab.AntourageFab.Companion.ARGS_STREAM_SELECTED
import com.google.firebase.FirebaseApp


class AntourageActivity : AppCompatActivity(), NetworkStateReceiver.NetworkStateReceiverListener  {


    companion object {
        const val ACTION_CONNECTION_LOST = "action_internet_connection_lost"
        const val ACTION_CONNECTION_AVAILABLE = "action_internet_connection_available"
        var isNetworkAvailable:Boolean = true
        fun initAntourage(context: Context){
            FirebaseApp.initializeApp(context)
        }
    }
    private lateinit var networkStateReceiver: NetworkStateReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_antourage)
        BASE_URL = UserCache.newInstance().getBeChoice(this)!!
        if(intent?.extras?.getParcelable<StreamResponse>(ARGS_STREAM_SELECTED) != null){
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainContent, WeaverFragment.newInstance(intent.getParcelableExtra(ARGS_STREAM_SELECTED))).commit()
        } else
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainContent, VideoListFragment.newInstance()).commit()
        FirebaseLoginService(this).handleSignIn()
        setupKeyboardListener(findViewById(R.id.mainContent))
        networkStateReceiver = NetworkStateReceiver()
        networkStateReceiver.addListener(this)
        this.registerReceiver(networkStateReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun networkAvailable() {
        isNetworkAvailable = true
        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        val intent = Intent(ACTION_CONNECTION_AVAILABLE)
        localBroadcastManager.sendBroadcast(intent)
    }

    override fun networkUnavailable() {
        isNetworkAvailable = false
        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        val intent = Intent(ACTION_CONNECTION_LOST)
        localBroadcastManager.sendBroadcast(intent)
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun setupKeyboardListener(view: View) {

        if (!(view is EditText )) {
            view.setOnTouchListener { v, event ->
                hideSoftKeyboard()
                false
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setupKeyboardListener(view.getChildAt(i))
            }
        }
    }

    fun hideSoftKeyboard() {
        val inputMethodManager = getSystemService(
            Activity.INPUT_METHOD_SERVICE
        ) as InputMethodManager
        if ( currentFocus != null)
            inputMethodManager.hideSoftInputFromWindow(
                currentFocus?.windowToken, 0
            )

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
