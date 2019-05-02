package com.antourage.weaverlib.ui

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.antourage.weaverlib.R
import com.antourage.weaverlib.screens.base.AntourageActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton



class AntourageFab @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FloatingActionButton(context, attrs, defStyleAttr) {

    init {
        backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                context,
                R.color.bg_color
            )
        )
        setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_icon_logo))
        val intent = Intent(context, AntourageActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        setOnClickListener { context.startActivity(intent)}
    }

}