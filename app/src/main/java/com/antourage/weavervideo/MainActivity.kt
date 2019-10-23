package com.antourage.weavervideo

import android.os.Bundle
import android.support.multidex.MultiDex
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        //TODO: delete
        const val API_KEY_1 = "a5f76ee9-bc76-4f76-a042-933b8993fc2c"
        const val API_KEY_2 = "4ec7cb01-a379-4362-a3a4-89699c17dc32"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MultiDex.install(this)
        setContentView(R.layout.activity_main)
        antfab.authWith(API_KEY_2)
    }

    override fun onPause() {
        super.onPause()
        antfab.onPause()
    }

    override fun onResume() {
        super.onResume()
        antfab.onResume()
    }
}
