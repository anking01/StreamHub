package com.ankush.streamhub.ui.stream

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.*
import android.webkit.*
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.ankush.streamhub.R
import com.ankush.streamhub.StreamHubApp
import com.ankush.streamhub.data.model.ContentItem
import com.ankush.streamhub.databinding.ActivityStreamBinding
import com.ankush.streamhub.ui.SharedViewModel
import com.ankush.streamhub.ui.SharedViewModelFactory
import com.ankush.streamhub.util.*
import androidx.activity.viewModels
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class StreamActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStreamBinding

    private val viewModel: SharedViewModel by viewModels {
        SharedViewModelFactory(application as StreamHubApp)
    }

    private lateinit var item: ContentItem
    private var isBookmarked = false
    private var isFullscreen = false

    // ── JavaScript injected after page load to clean up ads ───
    private val CLEAN_JS = """
        (function() {
            var selectors = [
                'header','nav','.advertisement','.ads','#ad',
                '.cookie-banner','.popup','.overlay','.newsletter-signup',
                '.site-header','.page-header','[class*="cookie"]','[id*="cookie"]',
                '.sticky-nav','aside','.sidebar'
            ];
            selectors.forEach(function(sel) {
                document.querySelectorAll(sel).forEach(function(el) {
                    el.style.display = 'none';
                });
            });
            // Make iframes responsive
            document.querySelectorAll('iframe').forEach(function(f){
                f.style.width='100%'; f.style.maxWidth='100%';
            });
        })();
    """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStreamBinding.inflate(layoutInflater)
        setContentView(binding.root)

        item = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_CONTENT_ITEM, ContentItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_CONTENT_ITEM) as? ContentItem
        } ?: run { finish(); return }

        setupToolbar()
        setupWebView()
        setupButtons()
        setupBackNavigation()
        populateInfo()
        loadContent()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        binding.tvToolbarTitle.text = item.sourceName
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled     = true
                domStorageEnabled     = true
                loadWithOverviewMode  = true
                useWideViewPort       = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode      = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                cacheMode             = WebSettings.LOAD_DEFAULT
                userAgentString       = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36"
            }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    binding.progressBar.show()
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    binding.progressBar.hide()
                    // Inject JS to clean up page
                    evaluateJavascript(CLEAN_JS, null)
                }
                override fun onReceivedError(
                    view: WebView?, request: WebResourceRequest?, error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        binding.layoutError.show()
                        binding.webView.hide()
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    binding.progressBar.progress = newProgress
                    if (newProgress == 100) binding.progressBar.hide()
                }
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    enterFullscreen(view, callback)
                }
                override fun onHideCustomView() {
                    exitFullscreen()
                }
            }
        }
    }

    private fun setupButtons() {
        // Bookmark toggle
        binding.btnBookmark.setOnClickListener {
            viewModel.toggleBookmark(item)
            isBookmarked = !isBookmarked
            updateBookmarkIcon()
            Snackbar.make(
                binding.root,
                if (isBookmarked) "Bookmarked!" else "Bookmark removed",
                Snackbar.LENGTH_SHORT
            ).show()
        }

        // Share
        binding.btnShare.setOnClickListener {
            shareUrl(item.sourceUrl, item.title)
        }

        // Open in browser
        binding.btnBrowser.setOnClickListener {
            openInBrowser(item.sourceUrl)
        }

        // Retry on error
        binding.btnRetry.setOnClickListener {
            binding.layoutError.hide()
            binding.webView.show()
            loadContent()
        }

        // WebView navigation
        binding.btnBack.setOnClickListener {
            if (binding.webView.canGoBack()) binding.webView.goBack()
        }
        binding.btnForward.setOnClickListener {
            if (binding.webView.canGoForward()) binding.webView.goForward()
        }
        binding.btnRefresh.setOnClickListener {
            binding.webView.reload()
        }
    }

    private fun populateInfo() {
        binding.tvTitle.text       = item.title
        binding.tvSource.text      = item.sourceName
        binding.tvTime.text        = item.publishedAt.toRelativeTime()
        binding.tvDescription.text = item.description.takeIf { it.isNotBlank() }
            ?: "No description available"
        binding.tvDescription.showIf(item.description.isNotBlank())
        binding.chipLive.showIf(item.isLive)
        binding.chipType.text = item.type.label

        // Author
        binding.tvAuthor.text = item.author ?: ""
        binding.tvAuthor.showIf(!item.author.isNullOrBlank())

        // Duration
        val dur = item.duration?.toDurationString()
        binding.tvDuration.text = dur ?: ""
        binding.tvDuration.showIf(dur != null)

        // View count
        val views = item.viewCount?.toCompactCount()
        binding.tvViews.text = if (views != null) "👁 $views views" else ""
        binding.tvViews.showIf(views != null)

        // Initial bookmark state
        lifecycleScope.launch {
            isBookmarked = viewModel.bookmarkIds.value.contains(item.id)
            updateBookmarkIcon()
        }
    }

    private fun loadContent() {
        val url = item.contentUrl.takeIf { it.isNotBlank() } ?: item.sourceUrl
        binding.webView.loadUrl(url)
    }

    private fun updateBookmarkIcon() {
        binding.btnBookmark.setImageResource(
            if (isBookmarked) R.drawable.ic_bookmark_filled
            else R.drawable.ic_bookmark_outline
        )
    }

    // ── Fullscreen video support ──────────────────────────────

    private var fullscreenView: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null

    private fun enterFullscreen(view: View?, callback: WebChromeClient.CustomViewCallback?) {
        isFullscreen = true
        fullscreenView = view
        fullscreenCallback = callback

        binding.layoutFullscreen.addView(view)
        binding.layoutFullscreen.show()
        binding.appBar.hide()
        binding.contentScroll.hide()
        binding.webView.hide()
        binding.bottomControls.hide()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun exitFullscreen() {
        isFullscreen = false
        fullscreenCallback?.onCustomViewHidden()

        binding.layoutFullscreen.removeAllViews()
        binding.layoutFullscreen.hide()
        binding.appBar.show()
        binding.contentScroll.show()
        binding.webView.show()
        binding.bottomControls.show()

        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    isFullscreen -> exitFullscreen()
                    binding.webView.canGoBack() -> binding.webView.goBack()
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.webView.onPause()
    }

    override fun onDestroy() {
        binding.webView.apply {
            stopLoading()
            destroy()
        }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_CONTENT_ITEM = "extra_content_item"
    }

    // Helper: share URL
    private fun shareUrl(url: String, title: String) {
        startActivity(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title\n\n$url")
        }.let { Intent.createChooser(it, "Share via") })
    }

    private fun openInBrowser(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
    }
}
