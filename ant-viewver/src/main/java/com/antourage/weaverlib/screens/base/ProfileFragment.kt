package com.antourage.weaverlib.screens.base

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.antourage.weaverlib.PropertyManager
import com.antourage.weaverlib.PropertyManager.Companion.PROFILE_URL
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.networking.auth.AuthClient
import com.antourage.weaverlib.other.networking.auth.AuthClient.ANONYMOUS_CLIENT_ID
import com.antourage.weaverlib.other.networking.auth.AuthClient.ANONYMOUS_SECRET
import com.antourage.weaverlib.other.networking.auth.AuthClient.CLIENT_ID
import kotlinx.android.synthetic.main.fragment_profile.*

class ProfileFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile,container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        closeButton.setOnClickListener {
            it.isEnabled = false
            parentFragmentManager.popBackStack()
        }

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.loadUrl("${PropertyManager.getInstance()?.getProperty(PROFILE_URL)}#/profile?token=${
            UserCache.getInstance()?.getAccessToken()
        }&idToken=${UserCache.getInstance()?.getIdToken()}&appClientId=$CLIENT_ID&appClientId=$CLIENT_ID")
    }
}