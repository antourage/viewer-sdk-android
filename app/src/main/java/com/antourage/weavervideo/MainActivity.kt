package com.antourage.weavervideo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.antourage.weaverlib.other.networking.base.AppExecutors
import com.antourage.weaverlib.ui.fab.AntourageFabLifecycleObserver

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AntourageFabLifecycleObserver.registerLifecycle(lifecycle)
    }
}
