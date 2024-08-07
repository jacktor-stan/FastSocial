package com.fasturl.fastsocial

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.webkit.GeolocationPermissions
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.SafeBrowsingResponseCompat
import androidx.webkit.WebResourceErrorCompat
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.fasturl.fastsocial.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.concurrent.Executor


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    var filePath: ValueCallback<Array<Uri>>? = null
    var mGeoLocationRequestOrigin: String? = null
    var mGeoLocationCallback: GeolocationPermissions.Callback? = null

    private var requestCode = 0


    companion object {
        var instance: MainActivity? = null
        val TAG: String = MainActivity::class.java.simpleName
    }


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            if (it.value) {
                //Toast.makeText(
                //  this, it.value.toString(), Toast.LENGTH_SHORT
                //).show()

                when (requestCode) {
                    1 -> {
                        if (checkPermission(1)) mGeoLocationCallback?.invoke(
                            mGeoLocationRequestOrigin,
                            true,
                            false
                        ) else mGeoLocationCallback?.invoke(mGeoLocationRequestOrigin, false, false)

                    }

                    2 -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            //
                        } else if (checkPermission(2)) {
                            permissionBlockedDialog(R.string.storage_permission_not_granted)
                        }
                    }
                }
            }
        }
    }


    val getFile = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_CANCELED) {
            filePath?.onReceiveValue(null)
        } else if (it.resultCode == Activity.RESULT_OK && filePath != null) {
            filePath!!.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(it.resultCode, it.data)
            )
            filePath = null
        }
    }

    @SuppressLint("AddJavascriptInterface", "SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)

        installSplashScreen()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)




        instance = this

        //Tombol back
        //Periksa jika berada di laman lain fungsikan sebagai tombol kembali bukan keluar
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    finish()
                }
            }
        })

        /*binding.swipeRefreshLayout.setOnRefreshListener {
            binding.webView.reload()
            binding.swipeRefreshLayout.isRefreshing = false
        }*/

        /*if (WebViewFeature.isFeatureSupported(WebViewFeature.TRACING_CONTROLLER_BASIC_USAGE)) {
            val tracingController = TracingController.getInstance()
            tracingController.start(
                TracingConfig.Builder().addCategories(CATEGORIES_WEB_DEVELOPER).build()
            )
        }*/

        binding.webView.apply {
            settings.apply {
                // aktifkan Javascript
                javaScriptEnabled = true

                /*
                 * Sets whether the WebView should support zooming using its on-screen zoom controls and gestures.
                 * The particular zoom mechanisms that should be used can be set with setBuiltInZoomControls(boolean).
                 * This setting does not affect zooming performed using the WebView#zoomIn() and WebView#zoomOut() methods.
                 * The default is true.
                 */
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false // no zoom button

                loadWithOverviewMode = true
                useWideViewPort = true

                domStorageEnabled = true

                userAgentString = getString(R.string.app_name)
            }
        }

        binding.webView.webViewClient = CustomWebViewClient(this)
        binding.webView.webChromeClient = CustomWebChromeClient(this)

        if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROCESS)) {
            Log.d(TAG, "isMultiProcessEnabled: " + WebViewCompat.isMultiProcessEnabled())
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING)) {
            WebViewCompat.startSafeBrowsing(this.applicationContext) { value ->
                Log.d(TAG, "WebViewCompat.startSafeBrowsing: $value")
            }
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            // If proxy1.com fails, try proxy2.com in order from the top
            val proxyConfig = ProxyConfig.Builder().addProxyRule("proxy1.cloud")
                .addProxyRule("proxy2.cloud", ProxyConfig.MATCH_HTTP)
                .addProxyRule("proxy3.cloud", ProxyConfig.MATCH_HTTPS)
                .addBypassRule("fasturl.*") // プロキシ設定除外のホスト
                .build()

            // Executor for listener
            val executor = Executor { Log.d(TAG, "${Thread.currentThread().name} : executor") }

            // Called when a proxy setting change is accepted? It's like you're not called...
            val listener = Runnable { Log.d(TAG, "${Thread.currentThread().name} : listener") }

            // Override WebView proxy settings from system settings
            ProxyController.getInstance().setProxyOverride(proxyConfig, executor, listener)

            // restore system settings
            ProxyController.getInstance().clearProxyOverride(executor, listener)
        }

        //if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
        //WebSettingsCompat.getForceDark(binding.webView.settings)
        //WebSettingsCompat.setForceDark(binding.webView.settings, WebSettingsCompat.FORCE_DARK_AUTO)
        //WebSettingsCompat.setForceDark(binding.webView.settings, WebSettingsCompat.FORCE_DARK_OFF)
        //WebSettingsCompat.setForceDark(binding.webView.settings, WebSettingsCompat.FORCE_DARK_ON)
        //}

        binding.webView.loadUrl("https://fasturl.cloud/FastSocial")

        // How to display local HTML, Ya makanya di kasih tau :v
        /*
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", AssetsPathHandler(this)) // Registering the main/assets directory
            .addPathHandler("/res/", ResourcesPathHandler(this)) // Register main/res directory
            .build()

        binding.webView.webViewClient = object : WebViewClient() {
            // Hook the request URL and display the local file
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }
        }

       // app default domain is "apppassets.androidplatform.net"
        // Load main/assets/www/index.html
        binding.webView.loadUrl("https://appassets.androidplatform.net/assets/www/index.html")
        */
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
    }

    override fun onPause() {
        binding.webView.onPause()
        super.onPause()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && binding.webView.canGoBack()) {
            binding.webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private class CustomWebViewClient(private val activity: MainActivity) :
        WebViewClientCompat() {

        override fun onPageCommitVisible(view: WebView, url: String) {
            super.onPageCommitVisible(view, url)
            Log.d(TAG, "onPageCommitVisible: $url")
        }

        override fun onReceivedError(
            view: WebView, request: WebResourceRequest, error: WebResourceErrorCompat
        ) {
            super.onReceivedError(view, request, error)
            Log.d(TAG, "onReceivedError: $error")
        }

        override fun onReceivedHttpError(
            view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse
        ) {
            super.onReceivedHttpError(view, request, errorResponse)
            Log.d(TAG, "onReceivedHttpError: $errorResponse")
        }

        override fun onSafeBrowsingHit(
            view: WebView,
            request: WebResourceRequest,
            threatType: Int,
            callback: SafeBrowsingResponseCompat
        ) {
            super.onSafeBrowsingHit(view, request, threatType, callback)
            Log.d(TAG, "onSafeBrowsingHit: $threatType")
        }

        override fun shouldOverrideUrlLoading(
            view: WebView, request: WebResourceRequest
        ): Boolean {
            Log.d(TAG, "shouldOverrideUrlLoading: ${request.url}")
            return false
        }


        // Notified when the screen (URL) changes due to actions such as clicking or returning
        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            super.doUpdateVisitedHistory(view, url, isReload)
            Log.d(TAG, "doUpdateVisitedHistory: $activity.binding.webView.url")
        }
    }

    private class CustomWebChromeClient(private val activity: MainActivity) :
        WebChromeClient() {

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {

            activity.filePath = filePathCallback
            val contentIntent = Intent(Intent.ACTION_GET_CONTENT)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (activity.checkPermission(0)) {
                    contentIntent.type = "*/*"
                    contentIntent.addCategory(Intent.CATEGORY_OPENABLE)
                    activity.getFile.launch(contentIntent)
                } else {
                    activity.permissionBlockedDialog(R.string.storage_permission_not_granted)
                    //activity.requestPermission(2)
                }
            } else {
                if (activity.checkPermission(2)) {
                    contentIntent.type = "*/*"
                    contentIntent.addCategory(Intent.CATEGORY_OPENABLE)
                    activity.getFile.launch(contentIntent)
                } else {
                    activity.permissionBlockedDialog(R.string.storage_permission_not_granted)
                    activity.requestPermission(2)
                }

            }
            return true
        }


        override fun onGeolocationPermissionsShowPrompt(
            origin: String,
            callback: GeolocationPermissions.Callback
        ) {
            //var mGeoLocationRequestOrigin = null
            //var mGeoLocationCallback = null
            // Do We need to ask for permission?
            if (!activity.checkPermission(1)) {

                MaterialAlertDialogBuilder(activity).apply {
                    setMessage("Diperlukan izin Lokasi, Tim developer harap berikan keterangan apa tujuan akses lokasi ini?")
                    setNegativeButton(R.string.ok) { _, _ ->
                        activity.mGeoLocationRequestOrigin = origin
                        activity.mGeoLocationCallback = callback
                        activity.requestPermission(1)
                    }

                    setPositiveButton(activity.getString(R.string.dialog_button_close)) { dialog, _ ->
                        dialog.cancel()
                    }
                    show()
                }
            } else {
                // Tell the WebView that permission has been granted
                callback.invoke(origin, true, false)
            }
        }


        // required to run JavaScript
        override fun onJsAlert(
            view: WebView?, url: String?, message: String?, result: JsResult?
        ): Boolean {
            return false
        }

        // required to run JavaScript
        override fun onJsPrompt(
            view: WebView?,
            url: String?,
            message: String?,
            defaultValue: String?,
            result: JsPromptResult?
        ): Boolean {
            return false
        }

        // needed to react <a target="_blank">
        override fun onCreateWindow(
            view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?
        ): Boolean {
            view ?: return false

            val href = view.handler.obtainMessage()
            view.requestFocusNodeHref(href)
            val url = href.data.getString("url")

            view.stopLoading()
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            activity.startActivity(browserIntent)
            return true
        }
    }


    //Checking if particular permission is given or not
    fun checkPermission(permission: Int): Boolean {
        when (permission) {
            0 -> return true
            1 -> return ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            2 -> return ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            3 -> return ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        }
        return false
    }


    private fun requestPermission(permission: Int) {
        when (permission) {
            1 -> {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
                requestCode = permission
            }

            2 -> {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                )
                requestCode = permission
            }
        }
    }

    private fun permissionBlockedDialog(msg: Int) {
        MaterialAlertDialogBuilder(this).apply {
            setMessage(msg)
            setNegativeButton(R.string.open_settings) { _, _ ->

                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val uri: Uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }

            setPositiveButton(getString(R.string.dialog_button_close)) { dialog, _ ->
                dialog.cancel()
            }

            setCancelable(false)

            show()
        }

    }
}