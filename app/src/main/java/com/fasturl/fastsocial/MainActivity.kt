package com.fasturl.fastsocial

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.KeyEvent
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.SafeBrowsingResponseCompat
import androidx.webkit.TracingConfig
import androidx.webkit.TracingConfig.CATEGORIES_WEB_DEVELOPER
import androidx.webkit.TracingController
import androidx.webkit.WebResourceErrorCompat
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.fasturl.fastsocial.databinding.ActivityMainBinding
import java.util.concurrent.Executor


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        val TAG: String = MainActivity::class.java.simpleName
    }

    @SuppressLint("AddJavascriptInterface", "SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.webView.reload()
            binding.swipeRefreshLayout.isRefreshing = false
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.TRACING_CONTROLLER_BASIC_USAGE)) {
            val tracingController = TracingController.getInstance()
            tracingController.start(
                TracingConfig.Builder().addCategories(CATEGORIES_WEB_DEVELOPER).build()
            )
        }

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

        binding.webView.loadUrl("https://fasturl.cloud/FastSocial/")

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
        // main/assets/www/index.htmlをロード
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

    private class CustomWebViewClient(private val activity: MainActivity) : WebViewClientCompat() {

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
}