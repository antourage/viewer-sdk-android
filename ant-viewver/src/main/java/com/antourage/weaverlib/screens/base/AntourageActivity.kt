package com.antourage.weaverlib.screens.base

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.antourage.weaverlib.ConfigManager
import com.antourage.weaverlib.Global
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.ContextWrapper
import com.antourage.weaverlib.other.networking.auth.AuthClient
import com.antourage.weaverlib.screens.web.PreFeedFragment
import com.antourage.weaverlib.ui.fab.AntourageFab
import com.antourage.weaverlib.ui.keyboard.KeyboardVisibilityEvent

class AntourageActivity : AppCompatActivity() {
    internal var keyboardIsVisible = false
        private set

    private var triggerKeyboardCallback = true
    var shouldGoBackToList = false
    var shouldHideBackButton = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_antourage_poc)
        if (UserCache.getInstance() == null || !ConfigManager.isConfigInitialized()) {
            AntourageFab.reconfigure(applicationContext)
        }
        registerKeyboardVisibilityEvent()
        intent?.data?.let {
            AuthClient.handleSignIn(it)
        }

        supportFragmentManager.beginTransaction()
            .replace(
                R.id.mainContent,
                PreFeedFragment()
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

    override fun attachBaseContext(newBase: Context) {
        val newLocale = Global.chosenLocale
        if (newLocale != null) {
            super.attachBaseContext(ContextWrapper.wrap(newBase, newLocale))
        } else {
            super.attachBaseContext(newBase)
        }
    }
}
