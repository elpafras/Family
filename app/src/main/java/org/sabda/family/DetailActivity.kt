package org.sabda.family

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.sabda.family.utility.LoadingUtil
import org.sabda.family.utility.StatusBarUtil

class DetailActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var webView: WebView
    private lateinit var loadingTextView: TextView
    private lateinit var loadingUtil: LoadingUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        StatusBarUtil().setLightStatusBar(this, R.color.white)
        initView()
        setupButtons()
        setupWebView()
    }

    private fun initView() {
        backButton      = findViewById(R.id.back)
        webView         = findViewById(R.id.webView)
        loadingUtil     = LoadingUtil()
        loadingTextView = TextView(this).apply {
            text = R.string.loading.toString()
            textSize = 18f
            setTextColor(ContextCompat.getColor(this@DetailActivity, android.R.color.black))
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { visibility = View.GONE }
        }
    }

    private fun setupButtons() {
        backButton.setOnClickListener { finish() }
    }

    private fun setupWebView(){
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                loadingUtil.showLoadingWebView(loadingTextView)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                loadingUtil.hideLoadingWebView(loadingTextView)
            }
        }

        val familyId = intent.getStringExtra("familyId")
        var extraText = intent.getStringExtra("Extra_Text")

        extraText = extraText?.replace(Regex("Bacaan:\\s*"), "")

        Log.d("cek XTRA", "setupWebView: $extraText")

        val url = if (!familyId.isNullOrEmpty()){
            "https://family.sabda.org/m-detail.php?id=$familyId"
        } else if (!extraText.isNullOrEmpty()) {
            "https://alkitab.sabda.org/?$extraText"
        } else {
            throw IllegalArgumentException("Tidak ada data yang valid untuk menampilkan halaman.")
        }

        webView.loadUrl(url)
    }
}