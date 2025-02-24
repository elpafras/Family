package org.sabda.family

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import org.sabda.family.base.BaseActivity
import org.sabda.family.databinding.ActivityDetailBinding
import org.sabda.family.utility.LoadingUtil
import org.sabda.family.utility.NetworkUtil
import org.sabda.family.utility.StatusBarUtil

class DetailActivity : BaseActivity<ActivityDetailBinding>() {

    override fun setupViewBinding(): ActivityDetailBinding {
        return ActivityDetailBinding.inflate(layoutInflater)
    }

    private lateinit var loadingTextView: TextView
    private val loadingUtil = LoadingUtil()

    private val isNightMode: Boolean
        get() = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private var homeUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StatusBarUtil().setLightStatusBar(this, R.color.white)
        setupButtons()

        checkInternetAndProceed {
            initView()
            setupWebView()
        }

    }

    private fun checkInternetAndProceed(action: () -> Unit) {
        if (NetworkUtil.isInternetAvailable(this)) {
            action()
        } else {
            NetworkUtil.showNoInternetDialog(this)
        }
    }

    private fun initView() {
        loadingTextView = TextView(this).apply {
            textSize = 18f
            setTextColor(
                if (isNightMode) resources.getColor(android.R.color.white, null)
                else resources.getColor(android.R.color.black, null)
            )
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
            }
        }
        binding.root.addView(loadingTextView)
        loadingTextView.visibility = View.GONE
    }

    private fun setupButtons() {
        binding.back.setOnClickListener { finish() }
    }

    private fun setupWebView(){
        val webView = binding.webView

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE

            allowFileAccess = false
            allowContentAccess = false
        }

        webView.webViewClient = object : WebViewClient() {


            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                loadingUtil.showLoadingView(loadingTextView)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                loadingUtil.hideLoadingView(loadingTextView)
            }
        }

        val sourceUrl = intent.getStringExtra("sourceUrl")

        if (sourceUrl.isNullOrEmpty()) {
            Toast.makeText(this, "URL tidak ditemukan", Toast.LENGTH_SHORT).show()
            Log.e("DetailActivity", "sourceUrl kosong atau null")
            finish() // Tutup activity jika tidak ada URL
            return
        }

        homeUrl = sourceUrl
        Log.d("DetailActivity", "Memuat URL: $sourceUrl")
        webView.loadUrl(sourceUrl)
        webView.clearCache(true)
        webView.clearHistory()
    }


}