package com.antourage.weaverlib.screens.base

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsIntent.Builder
import com.antourage.weaverlib.ConfigManager.ANONYMOUS_CLIENT_ID
import com.antourage.weaverlib.ConfigManager.ANONYMOUS_SECRET
import com.antourage.weaverlib.ConfigManager.CLIENT_ID
import com.antourage.weaverlib.ConfigManager.WEB_PROFILE_URL
import com.antourage.weaverlib.Global
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.ContextWrapper
import com.antourage.weaverlib.other.LiveWatchedBeforeSignIn.duration
import com.antourage.weaverlib.other.LiveWatchedBeforeSignIn.liveWatchedBeforeSignIn
import com.antourage.weaverlib.other.LiveWatchedBeforeSignIn.resetLastWatchedLive
import com.antourage.weaverlib.other.networking.VideoCloseBackUp
import com.antourage.weaverlib.other.networking.auth.AuthClient
import com.antourage.weaverlib.other.replaceFragment
import com.antourage.weaverlib.screens.list.VideoListFragment
import com.antourage.weaverlib.screens.vod.VodPlayerFragment
import com.antourage.weaverlib.screens.weaver.PlayerFragment
import com.antourage.weaverlib.ui.fab.AntourageFab
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
        if (UserCache.getInstance() == null) {
            AntourageFab.configure(applicationContext)
        }
        registerKeyboardVisibilityEvent()
        intent?.data?.let {
            AuthClient.handleSignIn(it)
        }
        VideoCloseBackUp.init(
            applicationContext.getSharedPreferences(
                "backUpPrefs",
                Context.MODE_PRIVATE
            )
        )

        val streamToWatch = liveWatchedBeforeSignIn ?: intent?.getParcelableExtra(ARGS_STREAM_SELECTED)
        shouldGoBackToList = streamToWatch != null
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.mainContent,
                if (streamToWatch != null) {
                    if (streamToWatch.isLive) {
                        PlayerFragment.newInstance(
                            streamToWatch,
                            UserCache.getInstance(applicationContext)?.getUserId(),
                            lastWatchedDuration = duration
                        )
                    } else {
                        VodPlayerFragment.newInstance(streamToWatch, isNewVod = true)
                    }
                } else VideoListFragment.newInstance()
            )
            .commit()
        resetLastWatchedLive()
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

    private fun onShowKeyboard(keyboardHeight: Int) {
        withCurrentFragment { it.onShowKeyboard(keyboardHeight) }
    }

    private fun onHideKeyboard(keyboardHeight: Int) {
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
            @Suppress("UNCHECKED_CAST")
            doStuff(currentFragment as BaseFragment<BaseViewModel>)
        }
    }

    // used in case video was open from FAB
    // to open initial list of streams
    private fun replaceListFragment() {
        replaceFragment(VideoListFragment.newInstance(), R.id.mainContent)
        shouldGoBackToList = false
    }

    fun openJoinTab() {
        val url =
        "${WEB_PROFILE_URL}#/auth?appClientId=${CLIENT_ID}&anonymousAppClientId=${ANONYMOUS_CLIENT_ID}&anonymousAppClientSecret=${ANONYMOUS_SECRET}"
        val builder = Builder()
        val customTabsIntent: CustomTabsIntent = builder
            .build()

        try {
            customTabsIntent.intent.setPackage("com.android.chrome")
            customTabsIntent.launchUrl(this, Uri.parse(url))
        } catch (e: ActivityNotFoundException) {
            customTabsIntent.intent.setPackage(null)
            customTabsIntent.launchUrl(this, Uri.parse(url))
        }
    }

    fun openProfileTab() {
        replaceFragment(
            ProfileFragment(),
            R.id.mainContent,
            addToBackStack = true,
            slideFromBottom = true
        )
    }

    override fun onBackPressed() {
        if (shouldGoBackToList) {
            replaceListFragment()
        } else {
            super.onBackPressed()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val newLocale = Global.chosenLocale
        if (newLocale != null) {
            super.attachBaseContext(ContextWrapper.wrap(newBase, newLocale))
        } else {
            super.attachBaseContext(newBase)
        }
    }
}
