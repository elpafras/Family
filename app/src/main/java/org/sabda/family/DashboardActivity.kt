package org.sabda.family

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import org.sabda.family.data.viewmodel.SharedViewModel
import org.sabda.family.databinding.ActivityDashboardBinding
import org.sabda.family.fragment.ChatFragment
import org.sabda.family.fragment.RenunganFragment
import org.sabda.family.fragment.ResourcesFragment
import org.sabda.family.utility.MenuUtil
import org.sabda.family.utility.NetworkUtil
import org.sabda.family.utility.StatusBarUtil

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val sharedViewModel: SharedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!NetworkUtil.isInternetAvailable(this)) {
            NetworkUtil.showNoInternetDialog(this)
        }

        StatusBarUtil().setLightStatusBar(this, R.color.white)
        setupButtons()
        setupNavBottom()

        if (savedInstanceState == null) {
            loadFragment(ResourcesFragment())
            binding.navBottom.selectedItemId = R.id.materi
        }

        sharedViewModel.setRenunganData(null)
    }

    private fun setupButtons() {
        binding.option.setOnClickListener { MenuUtil(this).setupMenu(it, this::class.java.simpleName) }
    }

    private fun setupNavBottom() {
        binding.navBottom.setOnItemSelectedListener { item: MenuItem ->
            var fragment: Fragment? = null
            val itemId = item.itemId
            when (itemId) {
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
            }

            loadFragment(fragment)
        }

        binding.navBottom.itemIconTintList = ColorStateList.valueOf(Color.WHITE)
        binding.navBottom.itemTextColor = ColorStateList.valueOf(Color.WHITE)

        val isNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        if (isNightMode) {
            binding.navBottom.itemActiveIndicatorColor = ColorStateList.valueOf(Color.parseColor("#4D4D4D"))
        } else {
            binding.navBottom.itemActiveIndicatorColor = ColorStateList.valueOf(Color.parseColor("#80BDBCBC"))
        }

    }

    private fun loadFragment(fragment: Fragment?): Boolean {
        if (fragment != null) {
            val tag = fragment::class.java.simpleName
            val currentFragment = supportFragmentManager.findFragmentByTag(tag)
            if (currentFragment == null) {
                supportFragmentManager.beginTransaction().replace(R.id.frameLayout, fragment, tag).commit()
            }
            return true
        }
        return false
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
}