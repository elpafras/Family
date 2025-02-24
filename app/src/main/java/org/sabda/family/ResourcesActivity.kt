package org.sabda.family

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sabda.family.base.BaseActivity
import org.sabda.family.data.repository.FamilyRepository
import org.sabda.family.databinding.ActivityResourcesBinding
import org.sabda.family.model.FamilyData
import org.sabda.family.utility.HtmlUtil
import org.sabda.family.utility.LoadingUtil
import org.sabda.family.utility.NetworkUtil
import org.sabda.family.utility.StatusBarUtil
import org.sabda.family.utility.YTPlayerUtil


class ResourcesActivity : BaseActivity<ActivityResourcesBinding>() {

    override fun setupViewBinding(): ActivityResourcesBinding {
        return ActivityResourcesBinding.inflate(layoutInflater)
    }

    private lateinit var familyRepository: FamilyRepository
    private lateinit var loadingTextView: TextView
    private val loadingUtil = LoadingUtil()

    private val isNightMode: Boolean
        get() = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StatusBarUtil().setLightStatusBar(this, R.color.white)
        familyRepository = FamilyRepository(this)

        setupLoadingTextView()
        binding.back.setOnClickListener { finish() }

        checkInternetAndProceed {
            setupButton()
            displayData()
        }
    }

    private fun checkInternetAndProceed(action: () -> Unit) {
        if (NetworkUtil.isInternetAvailable(this)) {
            action()
        } else {
            NetworkUtil.showNoInternetDialog(this)
        }
    }

    private fun setupButton() {
        binding.sourceButton.setOnClickListener { sourceAction() }
    }

    private fun sourceAction() {
        val sourceUrl = binding.sourceButton.tag as? String ?: ""

        Log.d(TAG, "Source URL: $sourceUrl")

        if (sourceUrl.isEmpty()) {
            Toast.makeText(this, "URL tidak tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        when {
            sourceUrl.contains("youtube.com") || sourceUrl.contains("youtu.be") -> {
                val ytPlayerUtil = YTPlayerUtil()
                ytPlayerUtil.showYoutubePopup(this@ResourcesActivity, sourceUrl)
            }
            sourceUrl.contains("instagram.com") -> {
                val intent = Intent(Intent.ACTION_VIEW, sourceUrl.toUri())
                intent.setPackage("com.instagram.android")
                startActivity(intent)
            }
            else -> {
                val intent = Intent(this, DetailActivity::class.java)
                intent.putExtra("sourceUrl", sourceUrl)
                startActivity(intent)
            }
        }
    }

    private fun setupLoadingTextView() {
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

    private fun displayData() {
        val familyId = intent.getStringExtra("familyId").orEmpty()
        Log.d(TAG, "checkfamilyid: $familyId ")
        if (familyId.isEmpty()) {
            Log.w(TAG, "displayData: No familyId provided")
            return
        }

        showLoading()

        lifecycleScope.launch {
            try {
                val resourcesMaterialList = withContext(Dispatchers.IO) {
                    familyRepository.fetchFamilyData(id = familyId)
                }
                val resourcesMaterial = resourcesMaterialList.firstOrNull()

                Log.d(TAG, "displayData: RM $resourcesMaterial")

                if (resourcesMaterial != null) {
                    updateUI(resourcesMaterial)
                    addToggleIcon(binding.short1, binding.shortTextView)
                    addToggleIcon(binding.summary, binding.summaryTextView)
                } else {
                    showDataNotFoundMessage()
                }
            } catch (e: Exception) {
                Log.e("ResourcesActivity", "Error fetching family data", e)
                showDataNotFoundMessage()
            } finally {
                hideLoading()
            }
        }
    }

    private fun showDataNotFoundMessage() {
        hideAllViews()
        Toast.makeText(this, "Data not found", Toast.LENGTH_SHORT).show()
    }

    private fun updateUI(data: FamilyData) {
        fun setTextOrHide(view: View, text: String?) {
            if (!text.isNullOrEmpty()) {
                (view as? TextView)?.text = text
                view.visibility = View.VISIBLE
            } else {
                view.visibility = View.GONE
            }
        }

        val seriesText = data.series.joinToString(", ")

        Log.d(TAG, "data summary: ${data.summary}")

        setTextOrHide(binding.seriesTextView, seriesText)
        setTextOrHide(binding.titleTextView, data.title)
        setTextOrHide(binding.authorTextView, data.author)
        setTextOrHide(binding.shortTextView, HtmlUtil.removeSpesificTags(data.short))
        setTextOrHide(binding.summaryTextView, HtmlUtil.removeSpesificTags(data.summary))

        binding.sourceButton.tag = data.source_url

        addLineViewBelow(binding.shortTextView)
        addLineViewBelow(binding.summaryTextView)
    }

    private fun hideAllViews() {
        listOf(
            binding.seriesTextView,
            binding.titleTextView,
            binding.authorTextView,
            binding.short1,
            binding.shortTextView,
            binding.summary,
            binding.summaryTextView
        ).forEach { it.visibility = View.GONE }
    }

    private fun showLoading() {
        loadingUtil.showLoadingView(loadingTextView)
        setViewsVisibility(View.GONE)
    }

    private fun hideLoading() {
        loadingUtil.hideLoadingView(loadingTextView)
        setViewsVisibility(View.VISIBLE)
    }

    private fun setViewsVisibility(visibility: Int) {
        binding.apply {
            seriesTextView.visibility = visibility
            titleTextView.visibility = visibility
            authorTextView.visibility = visibility
            short1.visibility = visibility
            shortTextView.visibility = visibility
            summary.visibility = visibility
            summaryTextView.visibility = visibility
            sourceButton.visibility = visibility
        }
    }

    private fun addLineViewBelow(targetView: View) {
        val parentLayout = targetView.parent as? ViewGroup ?: return

        val lineView = View(this).apply {
            layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 1).apply {
                addRule(RelativeLayout.BELOW, targetView.id)
                setMargins(10, 4, 10, 4)
            }
            setBackgroundColor(resources.getColor(R.color.black))
        }
        parentLayout.addView(lineView)
    }

    private fun addToggleIcon(toggleButton: View, targetView: View) {
        val parentLayout = toggleButton.parent as? RelativeLayout ?: return

        val toggleIcon = ImageView(this).apply {
            setImageDrawable(ContextCompat.getDrawable(this@ResourcesActivity, R.drawable.toggle_arrow))
            layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.ALIGN_TOP, toggleButton.id)
                marginEnd = (10 * resources.displayMetrics.density).toInt()
            }
        }
        parentLayout.addView(toggleIcon)

        val toggleClickListener = View.OnClickListener {
            val isVisible = targetView.isVisible
            animateRotation(toggleIcon, isVisible)
            animateVisibility(targetView, isVisible)
        }

        toggleButton.setOnClickListener(toggleClickListener)
        toggleIcon.setOnClickListener(toggleClickListener)
    }

    private fun animateRotation(view: View, isReversed: Boolean) {
        Log.d(TAG, "Animating rotation: isReversed=$isReversed")
        val fromDegrees = if (isReversed) 180f else 0f
        val toDegrees = if (isReversed) 0f else 180f
        val rotateAnimation = RotateAnimation(
            fromDegrees, toDegrees,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 300
            fillAfter = true
        }
        view.startAnimation(rotateAnimation)
    }

    private fun animateVisibility(view: View, isVisible: Boolean) {
        Log.d(TAG, "Animating visibility: isVisible=$isVisible")
        val alphaAnimation = if (isVisible) {
            AlphaAnimation(1f, 0f).apply { duration = 300 }
        } else {
            AlphaAnimation(0f, 1f).apply { duration = 300 }
        }
        alphaAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                view.visibility = if (isVisible) View.GONE else View.VISIBLE
            }
            override fun onAnimationRepeat(animation: Animation?) {}
        })
        view.startAnimation(alphaAnimation)
    }

    companion object {
        private const val TAG = "ResourcesActivity"
    }

}