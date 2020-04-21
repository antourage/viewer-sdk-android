package com.antourage.weaverlib.screens.base

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.ApiClient.BASE_URL
import com.antourage.weaverlib.screens.list.VideoListFragment
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog
import com.antourage.weaverlib.screens.vod.VodPlayerFragment
import com.antourage.weaverlib.screens.weaver.PlayerFragment
import com.antourage.weaverlib.ui.fab.AntourageFab.Companion.ARGS_STREAM_SELECTED
import com.antourage.weaverlib.ui.keyboard.KeyboardVisibilityEvent

class AntourageActivity : AppCompatActivity() {
    internal var keyboardIsVisible = false
        private set

    private var triggerKeyboardCallback = true
    private var shouldGoBackToList = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_antourage)
        registerKeyboardVisibilityEvent()
        BASE_URL = UserCache.getInstance(applicationContext)?.getBeChoice()
            ?: DevSettingsDialog.DEFAULT_URL

        val streamToWatch = intent?.getParcelableExtra<StreamResponse>(ARGS_STREAM_SELECTED)
        shouldGoBackToList = streamToWatch != null
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.mainContent,
                if (streamToWatch != null){
                    if(streamToWatch.isLive){
                        PlayerFragment.newInstance(streamToWatch, UserCache.getInstance(applicationContext)?.getUserId() ?: -1)
                    }
                    else{
                        VodPlayerFragment.newInstance(streamToWatch)
                    }
                }
                else VideoListFragment.newInstance()
            )
            .commit()

        FirebaseLoginService().handleSignIn()
        setupKeyboardListener(findViewById(R.id.mainContent))
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupKeyboardListener(view: View) {
        if (view !is EditText) {
            view.setOnTouchListener { _, _ ->
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
        val inputMethodManager =
            getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        if (currentFocus != null) {
            inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
    }

    private fun registerKeyboardVisibilityEvent() {
        KeyboardVisibilityEvent.setEventListener(this) { isOpen, height ->
            keyboardIsVisible = isOpen
            if (triggerKeyboardCallback) {
                if (isOpen) onShowKeyboard(height) else onHideKeyboard(height)
            } else {
                triggerKeyboardCallback = true
            }
        }
    }

    fun onShowKeyboard(keyboardHeight: Int) {
        withCurrentFragment { it.onShowKeyboard(keyboardHeight) }
    }

    fun onHideKeyboard(keyboardHeight: Int) {
        withCurrentFragment { it.onHideKeyboard(keyboardHeight) }
    }

    fun triggerKeyboardCallback(withCallback: Boolean) {
        triggerKeyboardCallback = withCallback
    }

    private fun withCurrentFragment(doStuff: (BaseFragment<BaseViewModel>) -> Unit) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.mainContent)
        if (currentFragment != null
            && currentFragment is BaseFragment<*>
            && currentFragment.isVisible
        ) {
            doStuff(currentFragment as BaseFragment<BaseViewModel>)
        }
    }

    // used in case video was open from FAB
    // to open initial list of streams
    private fun reopenActivity(){
        val intent = Intent(this, AntourageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        this.startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        if(shouldGoBackToList){
            reopenActivity()
        }else{
            super.onBackPressed()
        }
    }
}
