package com.antourage.weavervideo

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.multidex.MultiDex
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    //        lateinit var antfab: AntourageFab
    private var isUserAuthorized = false
    private lateinit var connectivityManager: ConnectivityManager

    companion object {
        const val TAG = "Antourage_testing_tag"
        const val TEST_API_KEY = "A5F76EE9-BC76-4F76-A042-933B8993FC2C"
//        const val TEST_API_KEY = "49D7E915-549B-4B79-9D61-FF5E5C85D2C2"
//        const val TEST_API_KEY = "4ec7cb01-a379-4362-a3a4-89699c17dc32"
//        const val TEST_API_KEY = "472EC909-BB20-4B86-A192-3A78C35DD3BA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MultiDex.install(this)
        setContentView(R.layout.activity_main)

        Picasso.get().load(R.drawable.hacken_header).into(header)
        Picasso.get().load(R.drawable.hacken_header_overlay).into(header_overlay)
        Picasso.get().load(R.drawable.hacken_footer).into(footer)
        Picasso.get().load(R.drawable.hacken_background).into(mainContent)

        connectivityManager =
            this@MainActivity.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager


//        /** To add widget programatically*/
//        antfab = AntourageFab(this)
//        if(antfab.parent == null){
//            antfab.setPosition("bottomRight")
//            antfab.showFab(this)
//            antfab.setLocale("sv")
//            antfab.setMargins(0, 0)
//        }

        //region Antourage authorization
        antfab.authWith(TEST_API_KEY.toUpperCase())
        //endregion
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
