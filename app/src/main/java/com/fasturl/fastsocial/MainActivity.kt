package com.fasturl.fastsocial

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.webkit.DownloadListener
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    var webView: WebView? = null
    var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // initialize the progressDialog
        progressDialog = ProgressDialog(this@MainActivity)
        progressDialog!!.setCancelable(true)
        progressDialog!!.setMessage("Loading...")
        progressDialog!!.show()


        //awokawok :v

        // get the web-view from the layout
        webView = findViewById(R.id.webView)

        // for handling Android Device [Back] key press
        webView?.canGoBackOrForward(99)

        // handling web page browsing mechanism
        webView?.setWebViewClient(myWebViewClient())

        // handling file upload mechanism
        webView?.setWebChromeClient(myWebChromeClient())

        // some other settings
        val settings = webView?.getSettings()
        settings!!.javaScriptEnabled = true
        settings.allowFileAccess = true
        settings.allowFileAccessFromFileURLs = true
        settings.setUserAgentString(WebView(this).settings.userAgentString)

        // set the download listener
        webView?.setDownloadListener(downloadListener)

        // load the website
        webView?.loadUrl("https://" + myWebSite)
    }

    // after the file chosen handled, variables are returned back to MainActivity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // check if the chrome activity is a file choosing session
        if (requestCode == file_chooser_activity_code) {
            if (resultCode == RESULT_OK && data != null) {
                var results: Array<Uri?>? = null

                // Check if response is a multiple choice selection containing the results
                if (data.clipData != null) {
                    val count = data.clipData!!.itemCount
                    results = arrayOfNulls(count)
                    for (i in 0 until count) {
                        results[i] = data.clipData!!.getItemAt(i).uri
                    }
                } else if (data.data != null) {
                    // Response is a single choice selection
                    results = arrayOf(data.data!!)
                }
                mUploadMessageArr!!.onReceiveValue(results as Array<Uri>)
                mUploadMessageArr = null
            } else {
                mUploadMessageArr!!.onReceiveValue(null)
                mUploadMessageArr = null
                Toast.makeText(this@MainActivity, "Error getting file", Toast.LENGTH_LONG).show()
            }
        }
    }

    internal inner class myWebViewClient : WebViewClient() {
        //        // ==============================
        //
        //        // for handling api lower than 24
        //        @SuppressWarnings("deprecation")
        //        @Override
        //        public boolean shouldOverrideUrlLoading(WebView view, String url) {
        //            final Uri uri = Uri.parse(url);
        //            return handleUri(uri);
        //        }
        //
        //        // for handling api higher than 24
        //        @TargetApi(Build.VERSION_CODES.N)
        //        @Override
        //        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        //            final Uri uri = request.getUrl();
        //            return handleUri(uri);
        //        }
        //
        //        private boolean handleUri(final Uri uri) {
        //            //Log.i(TAG, "Uri =" + uri);
        //            final String host = uri.getHost();
        //            final String scheme = uri.getScheme();
        //            // Based on some condition you need to determine if you are going to load the url
        //            // in your web view itself or in a browser.
        //            // You can use `host` or `scheme` or any part of the `uri` to decide.
        //            if (host == myWebSite) {
        //                // Returning false means that you are going to load this url in the webView itself
        //                return false;
        //            } else {
        //                // Returning true means that you need to handle what to do with the url
        //                // e.g. open web page in a Browser
        //                final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        //                startActivity(intent);
        //                return true;
        //            }
        //        }
        //
        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            //showing the progress bar once the page has started loading
            progressDialog!!.show()
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            // hide the progress bar once the page has loaded
            progressDialog!!.dismiss()
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            super.onReceivedError(view, request, error)
            // show the error message = no internet access
            webView!!.loadUrl("file:///android_asset/no_internet.html")
            // hide the progress bar on error in loading
            progressDialog!!.dismiss()
            Toast.makeText(applicationContext, "Internet issue", Toast.LENGTH_SHORT).show()
        }
    }

    // Calling WebChromeClient to select files from the device
    inner class myWebChromeClient : WebChromeClient() {
        @SuppressLint("NewApi")
        override fun onShowFileChooser(
            webView: WebView,
            valueCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)

            // set single file type, e.g. "image/*" for images
            intent.type = "*/*"

            // set multiple file types
            val mimeTypes = arrayOf("image/*", "application/pdf")
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            val chooserIntent = Intent.createChooser(intent, "Choose file")
            (webView.context as Activity).startActivityForResult(
                chooserIntent,
                file_chooser_activity_code
            )

            // Save the callback for handling the selected file
            mUploadMessageArr = valueCallback
            return true
        }
    }

    var downloadListener =
        DownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            progressDialog!!.dismiss()
            val i = Intent(Intent.ACTION_VIEW)

            // example of URL = https://www.pony.com/invoice.pdf
            i.data = Uri.parse(url)
            startActivity(i)
        }

    override fun onBackPressed() {
        if (webView!!.canGoBack()) {
            webView!!.goBack()
        } else {
            finish()
        }
    }

    companion object {
        // if your website starts with www, exclude it
        private const val myWebSite = "fasturl.cloud/FastSocial"

        // for handling file upload, set a static value, any number you like
        // this value will be used by WebChromeClient during file upload
        private const val file_chooser_activity_code = 1
        private var mUploadMessageArr: ValueCallback<Array<Uri>>? = null
    }
}