package com.antourage.weaverlib.screens.base

import android.Manifest
import android.Manifest.permission.*
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.antourage.weaverlib.Global
import com.antourage.weaverlib.PropertyManager
import com.antourage.weaverlib.PropertyManager.Companion.PROFILE_URL
import com.antourage.weaverlib.PropertyManager.Companion.WEB_PROFILE_URL
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.models.WebViewResponse
import com.antourage.weaverlib.other.networking.ConnectionStateMonitor
import com.antourage.weaverlib.other.networking.NetworkConnectionState
import com.antourage.weaverlib.other.networking.auth.AuthClient.CLIENT_ID
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_profile.*
import org.jetbrains.anko.backgroundColor


class ProfileFragment : Fragment() {

    private lateinit var snackBarBehaviour: BottomSheetBehavior<View>
    private var loaderAnimatedDrawable: AnimatedVectorDrawableCompat? = null
    private var isLoaderShowing = false

    private val REQUEST_SELECT_FILE = 100
    private val FILECHOOSER_RESULTCODE = 1
    private var uploadMessage: ValueCallback<Array<Uri>>? = null
    private var mUploadMessage: ValueCallback<*>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAndShowLoader()
        initSnackBar()
        initWebView()

        closeButton.setOnClickListener {
            it.isEnabled = false
            parentFragmentManager.popBackStack()
        }

        loadUrl()
    }



    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowContentAccess = true
        webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        webView.settings.allowFileAccess = true
        webView.webChromeClient = MyWebChromeClient()
        webView.addJavascriptInterface(JsObject(WebCallback.instance(
            { logout() }, { updateUser() })
        ), "AntListener"
        )
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                webView?.loadUrl(
                    "javascript:(function() {" +
                            "window.parent.addEventListener ('message', function(event) {" +
                            " AntListener.receiveMessage(JSON.stringify(event.data));});" +
                            "})()"
                )
                hideLoading()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (ConnectionStateMonitor.isNetworkAvailable()) {
                    showError()
                } else {
                    showNoConnection()
                }
            }
        }
    }

    private fun loadUrl() {
        webView.loadUrl(
            "${PropertyManager.getInstance()?.getProperty(WEB_PROFILE_URL)}#/profile?token=${
                UserCache.getInstance()?.getAccessToken()
            }&idToken=${
                UserCache.getInstance()?.getIdToken()
            }&refreshToken=${
                UserCache.getInstance()?.getRefreshToken()
            }&appClientId=$CLIENT_ID&appClientId=$CLIENT_ID"
        )
    }

    internal class JsObject(val callback: WebViewCallback) {
        @JavascriptInterface
        fun receiveMessage(jsonData: String?): Boolean {
            val message = Gson().fromJson(jsonData, WebViewResponse::class.java)
            callback.onMessage(message.type)
            return false
        }
    }

    private fun updateUser() {
        parentFragmentManager.popBackStack()
    }

    private fun logout() {
        UserCache.getInstance()?.logout()
        parentFragmentManager.popBackStack()
    }

    private fun subscribeToObservers() {
        ConnectionStateMonitor.internetStateLiveData.observe(
            this.viewLifecycleOwner,
            networkStateObserver
        )
    }

    private val networkStateObserver: Observer<NetworkConnectionState> = Observer { networkState ->
        if (networkState?.ordinal == NetworkConnectionState.AVAILABLE.ordinal) {
            resolveErrorSnackBar(R.string.ant_you_are_online)
            showLoading()
            webView?.reload()
        } else if (networkState?.ordinal == NetworkConnectionState.LOST.ordinal) {
            if (!Global.networkAvailable) {
                showNoConnection()
            }
        }
    }

    private fun resumeIfWasOffline() {
        if (ConnectionStateMonitor.isNetworkAvailable()) {
            if (snackBarBehaviour.state == BottomSheetBehavior.STATE_EXPANDED && errorSnackBar?.text == context?.resources?.getString(
                    R.string.ant_no_connection
                )
            ) {
                resolveErrorSnackBar(R.string.ant_you_are_online)
                showLoading()
                webView?.reload()
            }
        } else {
            showNoConnection()
        }
    }

    private fun resolveErrorSnackBar(messageId: Int) {
        if (snackBarBehaviour.state == BottomSheetBehavior.STATE_EXPANDED || snackBarBehaviour.state == BottomSheetBehavior.STATE_SETTLING) {
            context?.resources?.getString(messageId)
                ?.let { messageToDisplay ->
                    errorSnackBar?.text = messageToDisplay
                    errorSnackBar?.let { snackBar ->
                        val colorFrom: Int =
                            ContextCompat.getColor(requireContext(), R.color.ant_error_bg_color)
                        val colorTo: Int =
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.ant_error_resolved_bg_color
                            )
                        val duration = 500L
                        ObjectAnimator.ofObject(
                            snackBar, "backgroundColor",
                            ArgbEvaluator(), colorFrom, colorTo
                        )
                            .setDuration(duration)
                            .start()
                    }
                }
            Handler(Looper.getMainLooper()).postDelayed({ hideErrorSnackBar() }, 2000)
        }
    }

    private fun initSnackBar() {
        errorSnackBar.let { snackBar ->
            snackBarBehaviour = BottomSheetBehavior.from(snackBar as View)
            snackBarBehaviour.addBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {}

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                        //to disable user swipe
                        snackBarBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }
            })
        }
    }

    private fun showErrorSnackBar(message: String) {
        errorSnackBar?.text = message
        snackBarLayout?.visibility = View.VISIBLE
        if (snackBarBehaviour?.state != BottomSheetBehavior.STATE_EXPANDED) {
            context?.let {
                errorSnackBar?.backgroundColor =
                    ContextCompat.getColor(it, R.color.ant_error_bg_color)
            }
            snackBarBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun hideErrorSnackBar() {
        snackBarBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
        snackBarLayout?.visibility = View.INVISIBLE
    }

    private fun showError() {
        showErrorSnackBar(getString(R.string.ant_server_error))
        hideLoading()
        backgroundView.visibility = View.VISIBLE
        ivError.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.antourage_ic_error
            )
        )
        ivError.visibility = View.VISIBLE
    }

    private fun showNoConnection() {
        showErrorSnackBar(getString(R.string.ant_no_connection))
        hideLoading()
        backgroundView?.visibility = View.VISIBLE
        ivError?.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.antourage_ic_no_network
            )
        )
        ivError?.visibility = View.VISIBLE
    }

    internal interface WebViewCallback {
        fun onMessage(string: String)
    }

    internal object WebCallback : WebViewCallback {
        lateinit var update: () -> Unit
        lateinit var logout: () -> Unit


        fun instance(logout: () -> Unit, update: () -> Unit): WebViewCallback {
            this.update = update
            this.logout = logout
            return this
        }

        override fun onMessage(string: String) {
            when (string) {
                LOGOUT -> {
                    this.logout()
                }
                UPDATE_USER -> {
                    this.update()
                }
            }
        }

    }

    private fun initAndShowLoader() {
        loaderAnimatedDrawable = context?.let {
            AnimatedVectorDrawableCompat.create(
                it,
                R.drawable.antourage_loader_logo
            )
        }
        ivLoader?.setImageDrawable(loaderAnimatedDrawable)
        showLoading()
    }

    private fun showLoading() {
        ivError?.visibility = View.GONE
        loaderAnimatedDrawable?.apply {
            if (!isRunning) {
                registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                    override fun onAnimationEnd(drawable: Drawable?) {
                        ivLoader.post { start() }
                    }
                })
                start()
                isLoaderShowing = true
                ivLoader?.visibility = View.VISIBLE
            }
        }
    }

    private fun hideLoading() {
        loaderAnimatedDrawable?.apply {
            if (ivLoader?.visibility == View.VISIBLE && isRunning) {
                backgroundView?.visibility = View.GONE
                ivLoader?.visibility = View.INVISIBLE
                clearAnimationCallbacks()
                stop()
                isLoaderShowing = false
            }
        }
    }

    internal inner class MyWebChromeClient : WebChromeClient() {
        // For 3.0+ Devices (Start)
        // onActivityResult attached before constructor
        private fun openFileChooser(uploadMsg: ValueCallback<*>, acceptType: String) {
            mUploadMessage = uploadMsg
            val i = Intent(Intent.ACTION_GET_CONTENT)
            i.addCategory(Intent.CATEGORY_OPENABLE)
            i.type = "image/*"
            startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_RESULTCODE)
        }

        // For Lollipop 5.0+ Devices
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        override fun onShowFileChooser(
            mWebView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            if (uploadMessage != null) {
                uploadMessage!!.onReceiveValue(null)
                uploadMessage = null
            }

            uploadMessage = filePathCallback

            val intent = fileChooserParams.createIntent()
            try {
                startActivityForResult(intent, REQUEST_SELECT_FILE)
            } catch (e: Exception) {
                uploadMessage = null
                return false
            }

            return true
        }

        private fun openFileChooser(uploadMsg: ValueCallback<Uri>) {
            mUploadMessage = uploadMsg
            val i = Intent(Intent.ACTION_GET_CONTENT)
            i.addCategory(Intent.CATEGORY_OPENABLE)
            i.type = "image/*"
            startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_RESULTCODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == REQUEST_SELECT_FILE) {
                if (uploadMessage == null) return
                uploadMessage!!.onReceiveValue(
                    WebChromeClient.FileChooserParams.parseResult(
                        resultCode,
                        intent
                    )
                )
                uploadMessage = null
            }
        } else if (requestCode == FILECHOOSER_RESULTCODE) {
            if (null == mUploadMessage) return
            val result =
                if (intent == null || resultCode != RESULT_OK) null else intent.data
            mUploadMessage!!.onReceiveValue(result as Nothing?)
            mUploadMessage = null
        }
    }

    companion object {
        private const val UPDATE_USER = "updateUser"
        private const val LOGOUT = "logout"
    }

    override fun onResume() {
        super.onResume()
        subscribeToObservers()
        resumeIfWasOffline()
    }

    override fun onPause() {
        super.onPause()
        ConnectionStateMonitor.internetStateLiveData.removeObserver(networkStateObserver)
        loaderAnimatedDrawable?.apply {
            clearAnimationCallbacks()
            stop()
            isLoaderShowing = false

        }
    }
}