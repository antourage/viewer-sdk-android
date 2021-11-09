package com.antourage.weaverlib.screens.web

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.antourage.weaverlib.ConfigManager.CLIENT_ID
import com.antourage.weaverlib.ConfigManager.WEB_WIDGET_URL
import com.antourage.weaverlib.Global
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.networking.ConnectionStateMonitor
import com.antourage.weaverlib.other.networking.NetworkConnectionState
import com.antourage.weaverlib.screens.base.AntourageActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.fragment_pre_feed.*
import org.jetbrains.anko.backgroundColor
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*


class PreFeedFragment : Fragment() {

    private lateinit var snackBarBehaviour: BottomSheetBehavior<View>
    private var loaderAnimatedDrawable: AnimatedVectorDrawableCompat? = null
    private var isLoaderShowing = false

    private var uploadMessage: ValueCallback<Array<Uri>>? = null
    private val REQUEST_CODE_ALBUM = 1
    private val REQUEST_CODE_CAMERA = 2
    private var wasPaused = false
    private var shouldReload = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pre_feed, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAndShowLoader()
        initSnackBar()
        initWebView()
    }


    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowContentAccess = true
        webView.settings.allowFileAccess = true
        webView.isFocusable = true;
        webView.isFocusableInTouchMode = true;

        loadUrl()

        webView.webChromeClient = MyWebChromeClient()
        val jsObject = JsObject()
        webView.addJavascriptInterface(jsObject, "AntListener")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                webView?.loadUrl(
                    "javascript:(function() {" +
                            "window.parent.addEventListener ('message', function(event) {" +
                            " AntListener.receiveMessage(JSON.stringify(event.data));});" +
                            "})()"
                )
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (!wasPaused && webView?.progress == 100) {
                    shouldReload = false
                    hideLoading()
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                shouldReload = true
                if (!ConnectionStateMonitor.isNetworkAvailable()) {
                    showNoConnection()
                }
            }
        }
    }

    private fun loadUrl() {
        val lastViewDateUrl = if (UserCache.getInstance()?.getLastViewedTime() != null) {
            "?lastViewDate=" + URLEncoder.encode(
                UserCache.getInstance()?.getLastViewedTime(),
                "UTF-8"
            )
        } else {
            null
        }

        val url: String
        if (UserCache.getInstance()?.getAccessToken().isNullOrBlank() || UserCache.getInstance()
                ?.getIdToken().isNullOrBlank() || UserCache.getInstance()?.getRefreshToken()
                .isNullOrBlank()
        ) {
            url = "${WEB_WIDGET_URL}antourage-mobile/pre-feed"
            webView.loadUrl(
                url
            )
        } else {
            url = "${WEB_WIDGET_URL}antourage-mobile/pre-feed?token=${
                UserCache.getInstance()?.getAccessToken()
            }&idToken=${
                UserCache.getInstance()?.getIdToken()
            }&refreshToken=${
                UserCache.getInstance()?.getRefreshToken()
            }&appClientId=$CLIENT_ID" + if (lastViewDateUrl != null) "?lastViewDate=$lastViewDateUrl" else ""
            webView.loadUrl(
                url
            )
        }
    }

    inner class JsObject {
        @JavascriptInterface
        fun receiveMessage(jsonData: String?): Boolean {
            try {
                val jsonObject = JSONObject(jsonData)
                when (jsonObject.getString(TYPE)) {
                    BROWSER -> {
                        (activity as AntourageActivity).openBrowserTab(jsonObject.getString(URL))
                    }
                    LAST_VIEW_DATE -> {
                        val lastViewDate = jsonObject.getString(VIEW_DATE)
                        if (lastViewDate.isNotBlank() && lastViewDate != "null")
                            UserCache.getInstance()?.saveLastViewedTime(lastViewDate)
                    }
                    LOGOUT -> {
                        UserCache.getInstance()?.logout()
                    }
                    EXIT -> {
                        activity?.finish()
                    }
                    AUTH -> {
                        (activity as AntourageActivity).openJoinTab()
                    }
                }
            } catch (e: Exception) {
            }
            return false
        }
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
            if (shouldReload) {
                showLoading()
                webView?.reload()
            } else {
                backgroundView?.visibility = View.GONE
                ivLoader?.visibility = View.INVISIBLE
                isLoaderShowing = false
            }
        } else if (networkState?.ordinal == NetworkConnectionState.LOST.ordinal) {
            if (!Global.networkAvailable) {
                showNoConnection()
                if (webView?.progress != 100) {
                    shouldReload = true
                }
            }
        }
    }

    private fun resumeIfWasOffline() {
        if (ConnectionStateMonitor.isNetworkAvailable()) {
            if (snackBarBehaviour.state == BottomSheetBehavior.STATE_EXPANDED && profileSnackBar?.text == context?.resources?.getString(
                    R.string.ant_no_connection
                ) || (wasPaused && shouldReload)
            ) {
                resolveErrorSnackBar(R.string.ant_you_are_online)
                showLoading()
                webView?.reload()
            }
        } else {
            showNoConnection()
        }
        wasPaused = false
    }

    private fun resolveErrorSnackBar(messageId: Int) {
        if (snackBarBehaviour.state == BottomSheetBehavior.STATE_EXPANDED || snackBarBehaviour.state == BottomSheetBehavior.STATE_SETTLING) {
            context?.resources?.getString(messageId)
                ?.let { messageToDisplay ->
                    profileSnackBar?.text = messageToDisplay
                    profileSnackBar?.let { snackBar ->
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
        profileSnackBar.let { snackBar ->
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
        profileSnackBar?.text = message
        snackBarLayout?.visibility = View.VISIBLE
        if (snackBarBehaviour.state != BottomSheetBehavior.STATE_EXPANDED) {
            context?.let {
                profileSnackBar?.backgroundColor =
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
        context?.getString(R.string.ant_no_connection)?.let { showErrorSnackBar(it) }
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

        override fun getDefaultVideoPoster(): Bitmap? {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }

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
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "image/*"
                showChooserDialog(intent)
            } catch (e: Exception) {
                uploadMessage = null
                return false
            }

            return true
        }
    }

    private var dialog: Dialog? = null
    private var resetCallback = true
    private val PERMISSION_REQUEST_CODE = 200

    private fun showChooserDialog(intent: Intent) {
        if (dialog == null) {

            dialog = Dialog(requireContext())
            dialog?.setContentView(R.layout.dialog_chooser_layout)

            if (dialog?.window != null) {
                dialog?.window!!
                    .setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            }
            dialog?.setOnDismissListener {
                if (resetCallback && uploadMessage != null) {
                    uploadMessage!!.onReceiveValue(null)
                    uploadMessage = null
                }
                resetCallback = true
            }

            val closeButton = dialog!!.findViewById<ImageView>(R.id.closeBtn)
            closeButton.setOnClickListener {
                dialog!!.dismiss()
            }
            val fileButton = dialog!!.findViewById<TextView>(R.id.fileBtn)
            fileButton?.setOnClickListener {
                resetCallback = false
                dialog?.dismiss()
                startActivityForResult(intent, REQUEST_CODE_ALBUM)
            }

            val galleryButton = dialog!!.findViewById<TextView>(R.id.galleryBtn)
            galleryButton?.setOnClickListener {
                resetCallback = false
                dialog?.dismiss()
                val fileManagerIntent = Intent(Intent.ACTION_PICK)
                fileManagerIntent.setDataAndType(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    "image/*"
                )
                startActivityForResult(fileManagerIntent, REQUEST_CODE_ALBUM)
            }
            val cameraButton = dialog!!.findViewById<TextView>(R.id.cameraBtn)
            cameraButton?.setOnClickListener {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestStoragePermission()
                } else {
                    openCameraIntent()
                }
            }
        }
        dialog?.show()
    }

    private fun requestStoragePermission() {
        requestPermissions(
            arrayOf(Manifest.permission.CAMERA),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCameraIntent()
                } else if (!shouldShowRequestPermissionRationale(permissions[0])) {
                    val alertDialogBuilder = AlertDialog.Builder(requireContext())

                    alertDialogBuilder.setTitle("Permission needed")
                    alertDialogBuilder.setMessage("Permission needed for accessing camera")
                    alertDialogBuilder.setPositiveButton(
                        "Open Settings"
                    ) { _, _ ->
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts(
                            "package", requireActivity().packageName,
                            null
                        )
                        intent.data = uri
                        startActivity(intent)
                    }
                    alertDialogBuilder.setNegativeButton(
                        "Cancel"
                    ) { _, _ -> }

                    val dialog = alertDialogBuilder.create()
                    dialog.show()
                } else {
                    dialog?.dismiss()
                }
            }
        }
    }


    private fun openCameraIntent() {
        resetCallback = false
        dialog?.dismiss()
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (takePictureIntent.resolveActivity(activity?.packageManager!!) != null) {
            var photoFile: File? = null
            try {
                photoFile = getPhotoFileUri()
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
            if (photoFile != null) {
                val authority = "${requireContext().packageName}.antourage.fileProvider"
                val fileProviderUri = FileProvider.getUriForFile(
                    requireContext(), authority,
                    photoFile
                )
                mCameraPhotoPath = "file:" + photoFile.absolutePath
                takePictureIntent.putExtra(
                    MediaStore.EXTRA_OUTPUT, fileProviderUri
                )
                startActivityForResult(takePictureIntent, REQUEST_CODE_CAMERA)
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun getPhotoFileUri(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val fileName = "JPEG_$timeStamp.jpg"
        val mediaStorageDir =
            File(
                requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "user_images"
            )
        mediaStorageDir.mkdirs()
        return File(mediaStorageDir.path + File.separator + fileName)
    }

    private var mCameraPhotoPath: String? = null


    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        var results: Array<Uri>? = null
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_ALBUM -> {
                    val dataString: String = intent!!.dataString.toString()
                    results = arrayOf(Uri.parse(dataString))
                }
                REQUEST_CODE_CAMERA -> if (mCameraPhotoPath != null) {
                    results = arrayOf(Uri.parse(mCameraPhotoPath))
                }
            }
        }
        if (uploadMessage != null) {
            uploadMessage?.onReceiveValue(results)
            uploadMessage = null
        }
    }

    companion object {
        private const val TYPE = "type"
        private const val EXIT = "antourage-hideWidget"
        private const val AUTH = "antourage-auth"
        private const val BROWSER = "antourage-open-browser"
        private const val LAST_VIEW_DATE = "antourage-lastViewDate"
        private const val LOGOUT = "antourage-logout"
        private const val URL = "url"
        private const val VIEW_DATE = "lastViewDate"
    }

    override fun onResume() {
        super.onResume()
        subscribeToObservers()
        resumeIfWasOffline()
    }

    override fun onPause() {
        super.onPause()
        wasPaused = true
        webView?.stopLoading()
        ConnectionStateMonitor.internetStateLiveData.removeObserver(networkStateObserver)
        loaderAnimatedDrawable?.apply {
            if (ivLoader?.visibility == View.VISIBLE && isRunning) {
                ivLoader?.visibility = View.INVISIBLE
                clearAnimationCallbacks()
                stop()
                isLoaderShowing = false
            }
        }
    }
}