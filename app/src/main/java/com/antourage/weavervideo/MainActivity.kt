package com.antourage.weavervideo

import android.os.Bundle
import android.support.multidex.MultiDex
import android.support.v7.app.AppCompatActivity
import com.antourage.weaverlib.ui.fab.UserAuthResult
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MultiDex.install(this)
        setContentView(R.layout.activity_main)
        antfab.authWith(API_KEY, callback = {
            when (it) {
                is UserAuthResult.Success -> { }
                is UserAuthResult.Failure -> { }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        antfab.onResume()
    }

    override fun onPause() {
        super.onPause()
        antfab.onPause()
    }
}
