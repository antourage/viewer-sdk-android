package com.antourage.weaverlib.screens.base

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.antourage.weaverlib.R
import com.antourage.weaverlib.screens.videos.VideosFragment

class AntourageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_antourage)
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainContent,VideosFragment.newInstance()).commit()
    }
}
