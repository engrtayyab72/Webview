package com.android.cts.smsitis

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.w3c.dom.Text


class MainActivity : AppCompatActivity() {

    private lateinit var progressDialog: Dialog
    private lateinit var webview:WebView
    private val url = "https://smsitis.com/"
    private lateinit var tvNoInternet:TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Hide the status bar and the navigation bar
            window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        initProgressDialog()
        webview= findViewById<WebView>(R.id.webView)!!
        tvNoInternet= findViewById<TextView>(R.id.tvNoInternet)!!
        swipeRefreshLayout= findViewById<SwipeRefreshLayout>(R.id.swipeContainer)!!
        val webSettings = webview.settings
        webSettings.javaScriptEnabled = true

        webSettings.domStorageEnabled = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH) // Deprecated after API 18

        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false

        webview.webChromeClient = WebChromeClient()
        WebView.setWebContentsDebuggingEnabled(true)

        webSettings.loadsImagesAutomatically = true
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE

        webSettings.allowFileAccess = true


        webview.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                if(isInternetAvailable(this@MainActivity))
                {
                    tvNoInternet.visibility=View.GONE
                    showProgressDialog(true)
                }
                else
                {
                  tvNoInternet.visibility=View.VISIBLE
                }
                swipeRefreshLayout.isRefreshing = false
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                showProgressDialog(false)
                swipeRefreshLayout.isRefreshing = false
                webview.visibility = View.VISIBLE
                super.onPageFinished(view, url)
            }
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                handler?.proceed() // Ignore SSL certificate errors
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return false // Allow WebView to handle the URL loading
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return false // For API >= 21
            }
        }
        webview.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        webview.webChromeClient = WebChromeClient()
        webview.loadUrl(url)
        swipeRefreshLayout.setOnRefreshListener {
            webview.loadUrl(url)
         /*   if(isInternetAvailable(this@MainActivity))
            {
                tvNoInternet.visibility=View.GONE
                showProgressDialog(true)
                webview.reload()
            }
            else
            {
                tvNoInternet.visibility=View.VISIBLE
            }*/

        }
    }
    private fun initProgressDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.progress_dialog, null)
        progressDialog = Dialog(this).apply {
            setContentView(dialogView)
            window?.apply {
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

                setBackgroundDrawableResource(android.R.color.transparent)
            }
            setCancelable(false)
        }
    }

    private fun showProgressDialog(show: Boolean) {
        if (show) {
            progressDialog.show()
        } else {
            progressDialog.dismiss()
        }
    }
    override fun onBackPressed() {
        if (webview.canGoBack()) {
            webview.goBack()
        } else {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.really_exit))
                .setMessage(getString(R.string.sure))
                .setNegativeButton(R.string.no, null)
                .setPositiveButton(R.string.yes
                ) { arg0, arg1 ->
                    super.onBackPressed()
                    }.create().show()
        }
    }
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }

}