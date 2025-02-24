package org.sabda.family

import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.sabda.family.base.BaseActivity
import org.sabda.family.data.viewmodel.SharedViewModel
import org.sabda.family.databinding.ActivityDashboardBinding
import org.sabda.family.fragment.ChatFragment
import org.sabda.family.fragment.HomeFragment
import org.sabda.family.fragment.RenunganFragment
import org.sabda.family.fragment.ResourcesFragment
import org.sabda.family.utility.MenuUtil
import org.sabda.family.utility.NetworkUtil
import org.sabda.family.utility.StatusBarUtil

class DashboardActivity : BaseActivity<ActivityDashboardBinding>() {

    override fun setupViewBinding(): ActivityDashboardBinding {
        return ActivityDashboardBinding.inflate(layoutInflater)
    }

    private val sharedViewModel: SharedViewModel by viewModels()
    private var currentMenuId: Int = R.id.home

    private val menuOrder = mapOf(
        R.id.home to 0,
        R.id.chat to 1,
        R.id.materi to 2,
        R.id.renungan to 3
    )

    private val inactivityTimeout = 3000L // 3 detik
    private val handler = Handler(Looper.getMainLooper())
    private val hideFloatingButtonRunnable = Runnable {
        binding.floatingChatButton.animate()
            .alpha(0f)
            .setDuration(500)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction { binding.floatingChatButton.visibility = View.GONE }
            .start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!NetworkUtil.isInternetAvailable(this)) {
            NetworkUtil.showNoInternetDialog(this)
        }

        StatusBarUtil().setLightStatusBar(this, R.color.white)
        setupButtons()
        setupNavBottom()

        if (savedInstanceState == null) {
            loadFragment(HomeFragment(), R.id.home)
            binding.navBottom.selectedItemId = R.id.home
        }

        sharedViewModel.setRenunganData(null)

        resetInactivityTimer()
    }

    private fun setupButtons() {
        binding.option.setOnClickListener { MenuUtil(this).setupMenu(it, this::class.java.simpleName) }

        binding.floatingChatButton.setOnClickListener {
            val intent = Intent(this@DashboardActivity, MainActivity::class.java).apply {
                putExtra("START_NEW_CHAT", true)
            }
            startActivity(intent)
        }
    }

    private fun setupNavBottom() {
        binding.navBottom.setOnItemSelectedListener { item: MenuItem ->
            var fragment: Fragment? = null
            val itemId = item.itemId
            Log.d("NavBottom", "Selected item ID: $itemId")

            when (itemId) {
                R.id.home -> {
                    fragment = HomeFragment()
                    binding.toolbarTitle.text = getString(R.string.app_name)
                }
                R.id.chat -> {
                    fragment = ChatFragment()
                    binding.toolbarTitle.text = getString(R.string.fragment_chat)
                }
                R.id.materi -> {
                    fragment = ResourcesFragment()
                    binding.toolbarTitle.text = getString(R.string.fragment_resources)
                }
                R.id.renungan -> {
                    fragment = RenunganFragment()
                    binding.toolbarTitle.text = getString(R.string.fragment_renungan)
                }
                else -> false
            }

            loadFragment(fragment, item.itemId)
        }

        binding.navBottom.itemIconTintList = ColorStateList.valueOf(Color.WHITE)
        binding.navBottom.itemTextColor = ColorStateList.valueOf(Color.WHITE)

        val isNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        val activeColor = ContextCompat.getColor(
            this, if (isNightMode) R.color.night_nav_active else R.color.nav_active
        )
        binding.navBottom.itemActiveIndicatorColor = ColorStateList.valueOf(activeColor)

    }

    private fun loadFragment(fragment: Fragment?, newMenuId: Int): Boolean {
        fragment ?: return false

        val tag = fragment::class.java.simpleName
        Log.d("FragmentDebug", "Current fragment: ${supportFragmentManager.findFragmentByTag(tag)?.javaClass?.simpleName}")
        Log.d("FragmentDebug", "New fragment to load: $tag")

        //if (supportFragmentManager.findFragmentByTag(tag) != null) return false

        val transaction = supportFragmentManager.beginTransaction()

        // Tentukan arah animasi berdasarkan posisi menu
        val animation = when {
            (menuOrder[newMenuId] ?: 0) > (menuOrder[currentMenuId] ?: 0) ->
                R.anim.slide_in_left to R.anim.slide_out_left
            else ->
                R.anim.slide_in_right to R.anim.slide_out_right
        }

        transaction.setCustomAnimations(animation.first, animation.second)
            .replace(R.id.frameLayout, fragment, tag)
            .commit()

        currentMenuId = newMenuId // Perbarui menu aktif
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("selected_nav_item", binding.navBottom.selectedItemId)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val selectedItemId = savedInstanceState.getInt("selected_nav_item")
        binding.navBottom.selectedItemId = selectedItemId
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        resetInactivityTimer() // Setiap ada sentuhan, reset timer
        return super.dispatchTouchEvent(event)
    }

    private fun resetInactivityTimer() {
        handler.removeCallbacks(hideFloatingButtonRunnable)
        binding.floatingChatButton.visibility = View.VISIBLE
        binding.floatingChatButton.animate()
            .alpha(1f)
            .setDuration(500)
            .setInterpolator(DecelerateInterpolator())
            .start()
        handler.postDelayed(hideFloatingButtonRunnable, inactivityTimeout)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(hideFloatingButtonRunnable)
    }
}