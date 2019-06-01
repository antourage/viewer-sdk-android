package com.antourage.weaverlib.screens.base

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Rational
import android.view.View
import com.antourage.weaverlib.R
import com.antourage.weaverlib.screens.base.streaming.StreamingFragment
import com.antourage.weaverlib.screens.list.VideoListFragment
import android.content.res.Configuration


class AntourageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_antourage)
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainContent, VideoListFragment.newInstance()).commit()
    }

    override fun onUserLeaveHint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            && supportFragmentManager.findFragmentById(R.id.mainContent) is StreamingFragment<*>
        ) {
            enterPictureInPictureMode(
                with(PictureInPictureParams.Builder()) {
                    val width = 16
                    val height = 9
                    setAspectRatio(Rational(width, height))
                    build()
                })
        }
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
