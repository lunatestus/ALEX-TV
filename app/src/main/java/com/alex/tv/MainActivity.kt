package com.alex.tv

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.content.Intent
import android.webkit.WebChromeClient
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val bridgeExecutor = Executors.newSingleThreadExecutor()
    private var currentPage: String = "home"
    private var libraryPath: String = "/media"
    private var downloadId: Long = -1L

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                installApk()
            }
        }
    }

    private fun installApk() {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(intent)
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }

        // Fullscreen immersive
        supportActionBar?.hide()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            enableFileAccess(settings)

            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()

            addJavascriptInterface(NativeBridge(), "AndroidBridge")
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            setBackgroundColor(0xFF000000.toInt())
        }

        setContentView(webView)
        webView.loadUrl("file:///android_asset/index.html")

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBackPress()
                }
            }
        )
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        webView.pauseTimers()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        webView.resumeTimers()
    }

    override fun onDestroy() {
        unregisterReceiver(onDownloadComplete)
        webView.destroy()
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            handleBackPress()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun handleBackPress() {
        // Custom back handling: library folder -> parent, library root -> home, home -> exit
        when (currentPage) {
            "library" -> {
                val atRoot = libraryPath.isBlank() || libraryPath == "/media"
                if (atRoot) {
                    webView.evaluateJavascript("window.__goHome && window.__goHome()", null)
                } else {
                    webView.evaluateJavascript("window.__libraryBack && window.__libraryBack()", null)
                }
            }
            "home" -> {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
            else -> {
                webView.evaluateJavascript("window.__goHome && window.__goHome()", null)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun enableFileAccess(settings: WebSettings) {
        settings.allowFileAccessFromFileURLs = true
        settings.allowUniversalAccessFromFileURLs = true
    }

    private inner class NativeBridge {
        @JavascriptInterface
        fun fetchJson(url: String, callbackId: String) {
            bridgeExecutor.execute {
                val (ok, payload) = try {
                    true to fetchUrl(url)
                } catch (e: Exception) {
                    false to (e.message ?: "Network error")
                }
                val js = "window.__nativeFetchResolve(" +
                    "${JSONObject.quote(callbackId)}, ${if (ok) "true" else "false"}, ${JSONObject.quote(payload)}" +
                    ");"
                webView.post {
                    webView.evaluateJavascript(js, null)
                }
            }
        }

        @JavascriptInterface
        fun setNavState(page: String, path: String) {
            currentPage = page
            libraryPath = path
        }

        @JavascriptInterface
        fun play(url: String, title: String) {
            if (url.isBlank()) return
            runOnUiThread {
                val intent = Intent(this@MainActivity, PlayerActivity::class.java)
                intent.putExtra(PlayerActivity.EXTRA_STREAM_URL, url)
                intent.putExtra(PlayerActivity.EXTRA_TITLE, title)
                startActivity(intent)
            }
        }

        @JavascriptInterface
        fun updateApp(url: String) {
            if (url.isBlank()) return
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Downloading update...", Toast.LENGTH_SHORT).show()
                val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
                if (file.exists()) file.delete()
                
                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    setTitle("ALEX TV Update")
                    setDescription("Downloading new version")
                    setDestinationInExternalFilesDir(this@MainActivity, Environment.DIRECTORY_DOWNLOADS, "update.apk")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                }
                val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                downloadId = manager.enqueue(request)
            }
        }
    }

    private fun fetchUrl(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) throw IOException("HTTP $code $body")
            return body
        } finally {
            conn.disconnect()
        }
    }
}
