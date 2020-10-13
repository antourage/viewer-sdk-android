package com.antourage.weaverlib.other.ui

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText

class CustomEditText(context: Context?, attrs: AttributeSet?) : AppCompatEditText(context!!, attrs){
    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (!focused) hideKeyboard(this)
    }

    private fun hideKeyboard(view: View){
        val inputMethodManager = context
            .getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}