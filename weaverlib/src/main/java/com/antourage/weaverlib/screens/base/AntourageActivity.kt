package com.antourage.weaverlib.screens.base

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Rational
import com.antourage.weaverlib.R
import com.antourage.weaverlib.screens.base.streaming.StreamingFragment
import com.antourage.weaverlib.screens.list.VideoListFragment

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

}
