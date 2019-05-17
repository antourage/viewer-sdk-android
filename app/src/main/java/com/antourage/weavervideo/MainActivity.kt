package com.antourage.weavervideo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.antourage.weaverlib.other.networking.base.AppExecutors
import com.antourage.weaverlib.ui.fab.AntourageFabLifecycleObserver
import com.antourage.weaverlib.ui.fab.BadgeFab
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
