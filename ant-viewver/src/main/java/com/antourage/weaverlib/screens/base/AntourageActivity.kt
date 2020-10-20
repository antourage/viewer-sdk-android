package com.antourage.weaverlib.screens.base

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.antourage.weaverlib.Global
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.ContextWrapper
import com.antourage.weaverlib.other.isEmptyTrimmed
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.ApiClient.BASE_URL
import com.antourage.weaverlib.other.networking.VideoCloseBackUp
import com.antourage.weaverlib.other.replaceFragment
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
    var shouldGoBackToList = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_antourage)
        registerKeyboardVisibilityEvent()
        VideoCloseBackUp.init(
            applicationContext.getSharedPreferences(
                "backUpPrefs",
                Context.MODE_PRIVATE
            )
        )
        if (BASE_URL.isEmptyTrimmed()) BASE_URL =
            UserCache.getInstance(applicationContext)?.getBeChoice()
                ?: DevSettingsDialog.DEFAULT_URL

        val streamToWatch = intent?.getParcelableExtra<StreamResponse>(ARGS_STREAM_SELECTED)
        shouldGoBackToList = streamToWatch != null
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.mainContent,
                if (streamToWatch != null) {
                    if (streamToWatch.isLive) {
                        PlayerFragment.newInstance(
                            streamToWatch,
                            UserCache.getInstance(applicationContext)?.getUserId() ?: -1
                        )
                    } else {
                        VodPlayerFragment.newInstance(streamToWatch, isNewVod = true)
                    }
                } else VideoListFragment.newInstance()
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
    private fun replaceListFragment() {
        replaceFragment(VideoListFragment.newInstance(), R.id.mainContent)
        shouldGoBackToList = false
    }

    override fun onBackPressed() {
        if (shouldGoBackToList) {
            replaceListFragment()
        } else {
            super.onBackPressed()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val newLocale = Global.currentLocale
        if (newLocale != null) {
            super.attachBaseContext(ContextWrapper.wrap(newBase, newLocale))
        } else {
            super.attachBaseContext(newBase)
        }
    }
}
