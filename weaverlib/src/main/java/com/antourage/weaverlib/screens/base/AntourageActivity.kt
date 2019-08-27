package com.antourage.weaverlib.screens.base

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.ApiClient.BASE_URL
import com.antourage.weaverlib.screens.list.VideoListFragment
import com.antourage.weaverlib.screens.weaver.PlayerFragment
import com.antourage.weaverlib.ui.fab.AntourageFab.Companion.ARGS_STREAM_SELECTED

class AntourageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_antourage)
        BASE_URL = UserCache.newInstance().getBeChoice(this)!!
        if (intent?.extras?.getParcelable<StreamResponse>(ARGS_STREAM_SELECTED) != null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.mainContent,
                    PlayerFragment.newInstance(intent.getParcelableExtra(ARGS_STREAM_SELECTED))
                )
                .commit()
        } else
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainContent, VideoListFragment.newInstance()).commit()
        FirebaseLoginService().handleSignIn()
        setupKeyboardListener(findViewById(R.id.mainContent))
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupKeyboardListener(view: View) {

        if (!(view is EditText)) {
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
        if (currentFocus != null)
            inputMethodManager.hideSoftInputFromWindow(
                currentFocus?.windowToken, 0
            )

    }

    // in branch additional_features
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
