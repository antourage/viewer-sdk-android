package com.antourage.weavervideo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //AntourageFabLifecycleObserver.registerLifecycle(lifecycle)
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
