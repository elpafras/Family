package org.sabda.family.fragment

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sabda.family.DashboardActivity
import org.sabda.family.R
import org.sabda.family.ResourcesActivity
import org.sabda.family.adapter.FamilyPagerAdapter
import org.sabda.family.base.BaseFragment
import org.sabda.family.data.repository.FamilyRepository
import org.sabda.family.data.repository.RenunganRepository
import org.sabda.family.data.viewmodel.SharedViewModel
import org.sabda.family.databinding.FragmentHomeBinding
import org.sabda.family.model.FamilyData
import org.sabda.family.model.RenunganData
import org.sabda.family.utility.DialogUtil
import org.sabda.family.utility.HorizontalSpacingItemDecoration
import org.sabda.family.utility.LoadingUtil
import org.sabda.family.utility.NetworkUtil
import java.lang.ref.WeakReference


class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    companion object {
        private const val TAG = "HomeFragment"
    }

    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val renunganRepository by lazy { RenunganRepository(requireContext().applicationContext) }
    private val familyRepository by lazy { FamilyRepository(requireContext().applicationContext) }

    private var currentRenunganData: RenunganData? = null
    private val familyList = mutableListOf<FamilyData>()
    private lateinit var familyAdapter: FamilyPagerAdapter
    private lateinit var loadingTextView: TextView
    private val loadingUtil = LoadingUtil()

    private val isNightMode: Boolean
        get() = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private var autoScrollRunnable: AutoScrollRunnable? = null

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentHomeBinding.inflate(inflater, container, false)

    override fun onBackPressed() {
        // Misalnya tampilkan konfirmasi sebelum keluar dari aplikasi
        Toast.makeText(requireContext(), "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "HomeFragment resumed, fetching latest data...")
        fetchDisplayRenungan() // Memuat ulang data renungan
        fetchViewPagerData() // Memuat ulang data ViewPager
    }


    override fun onViewCreated( view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupLoadingTextView()
        setupClickedView()

        checkInternetAndProceed {
            fetchDisplayRenungan()
            fetchViewPagerData()
        }

    }

    private fun checkInternetAndProceed(action: () -> Unit) {
        if (NetworkUtil.isInternetAvailable(requireContext())) {
            action()
        } else {
            NetworkUtil.showNoInternetDialog(requireContext())
        }
    }

    private fun setupRecyclerView() {
        familyAdapter = FamilyPagerAdapter(emptyList()) { familyId ->
            val intent = Intent(context, ResourcesActivity::class.java).apply {
                putExtra("familyId", familyId)
            }
            startActivity(intent)
        }

        binding.horizontalRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = familyAdapter
            addItemDecoration(HorizontalSpacingItemDecoration(16))
        }

        PagerSnapHelper().attachToRecyclerView(binding.horizontalRecyclerView)
    }

    private fun setupClickedView() {
        binding.buttonRenungan.setOnClickListener {
            updateToolbarAndNav(getString(R.string.fragment_renungan), R.id.renungan)
            loadFragment(RenunganFragment())
        }
        binding.natsVerseNumber.setOnClickListener {
            DialogUtil.fetchDialogNats(
                requireContext(),
                lifecycleScope,
                renunganRepository,
                currentRenunganData?.nats_verse
            )
        }
    }

    private fun loadFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun updateToolbarAndNav(title: String, menuId: Int) {

        val dashboardActivity = requireActivity() as? DashboardActivity ?: return
        val toolbarTitle = dashboardActivity.findViewById<TextView>(R.id.toolbarTitle)
        val navBottom = dashboardActivity.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.navBottom)
        Log.d("NavBottom", "Selected Item: ${navBottom.selectedItemId}, Expected: $menuId")

        Log.d("NavBottom", "Sebelum update: selectedItemId = ${navBottom?.selectedItemId}, targetMenuId = $menuId")

        toolbarTitle?.text = title

        navBottom?.post {
            Log.d("NavBottom", "Update toolbar & nav dipanggil")

            // Reset selectedItemId untuk memaksa trigger perubahan
            navBottom.selectedItemId = R.id.dummy_menu_item // Pastikan ID ini tidak ada di menu
            navBottom.post {
                navBottom.selectedItemId = menuId
                navBottom.menu.findItem(menuId).isChecked = true
                Log.d("NavBottom", "Setelah update: selectedItemId = ${navBottom.selectedItemId}")
            }

            if (navBottom.selectedItemId == menuId) {
                Log.d("NavBottom", "User klik menu yang sama, akan refresh fragment")
                val fragmentManager = dashboardActivity.supportFragmentManager
                fragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, HomeFragment())
                    .commitAllowingStateLoss()
            }
        }
    }


    private fun fetchDisplayRenungan() {
        val existingRenungan = sharedViewModel.renunganData.value
        if (existingRenungan != null) {
            displayRenungan(existingRenungan)
            return
        }

        showLoading()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val renungan = withContext(Dispatchers.IO) { renunganRepository.fetchRenungan() }
                renungan?.let {
                    sharedViewModel.setRenunganData(it)
                    displayRenungan(it)
                }
            } finally {
                if (isAdded) hideLoading()
            }
        }
    }

    private fun displayRenungan(renunganData: RenunganData) {
        if (currentRenunganData == renunganData) return
        currentRenunganData = renunganData

        with(binding) {
            homeRenunganTitle.text  = renunganData.title
            homeNatsVerse.text      = renunganData.nats
            natsVerseNumber.apply {
                text                = renunganData.nats_verse
                paintFlags          = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            }
        }
    }

    private fun fetchViewPagerData() {
        val existingFamilyData = sharedViewModel.homeFamilyData.value
        if (!existingFamilyData.isNullOrEmpty()) {
            familyList.clear()
            familyList.addAll(existingFamilyData.shuffled())
            familyAdapter.updateData(familyList)

            if (familyAdapter.itemCount > 0) {
                setupAutoScroll()
            }
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val familyDataList = withContext(Dispatchers.IO) {
                    familyRepository.fetchFamilyData(fragmentName = "HomeFragment")
                }
                sharedViewModel.setHomeFamilyData(familyDataList)
                familyList.clear()
                familyList.addAll(familyDataList.shuffled())
                familyAdapter.updateData(familyList)
                familyAdapter.notifyDataSetChanged()

                if (familyAdapter.itemCount > 0) {
                    setupAutoScroll()
                }

            } catch (e: Exception) {
                Log.e("List_Data", "Error fetching family data", e)
            }
        }
    }

    private fun setupLoadingTextView() {
        binding.root.apply {
            loadingTextView = TextView(context).apply {
                textSize = 18f
                setTextColor(
                    if (isNightMode) resources.getColor(android.R.color.white, null)
                    else resources.getColor(android.R.color.black, null)
                )

                textAlignment = View.TEXT_ALIGNMENT_CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                }
            }
            this.addView(loadingTextView)
        }
        loadingTextView.visibility = View.GONE
    }

    private fun setViewsVisibility(visibility: Int) {
        if (!isAdded) return
        binding.apply {
            logoImageView.visibility = visibility
            textViewDescription.visibility = visibility
            homeRenunganTitle.visibility = visibility
            homeNatsVerse.visibility = visibility
            natsVerseNumber.visibility = visibility
            buttonRenungan.visibility = visibility
            resourcesTextView.visibility = visibility
        }
    }

    private fun showLoading() {
        if (!isAdded) return
        loadingUtil.showLoadingView(loadingTextView)
        setViewsVisibility(View.GONE)
    }

    private fun hideLoading() {
        if (!isAdded) return
        loadingUtil.hideLoadingView(loadingTextView)
        setViewsVisibility(View.VISIBLE)
    }

    private fun setupAutoScroll() {
        autoScrollRunnable?.let { autoScrollHandler.removeCallbacks(it) }

        autoScrollRunnable = AutoScrollRunnable(this)
        autoScrollHandler.postDelayed(autoScrollRunnable!!, 5000)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        (loadingTextView.parent as? ViewGroup)?.removeView(loadingTextView)

        autoScrollRunnable?.let {
            autoScrollHandler.removeCallbacks(it)
        }
        autoScrollRunnable = null
    }

    override fun onPause() {
        super.onPause()
        autoScrollRunnable?.let { autoScrollHandler.removeCallbacks(it) }
    }

    private class AutoScrollRunnable(fragment: HomeFragment) : Runnable {
        private val fragmentRef = WeakReference(fragment)

        override fun run() {
            val fragment = fragmentRef.get() ?: return
            if (!fragment.isAdded) return

            val layoutManager = fragment.binding.horizontalRecyclerView.layoutManager as? LinearLayoutManager
            layoutManager?.let {
                val firstVisibleItem = it.findFirstVisibleItemPosition()
                val nextPosition = (firstVisibleItem + 1) % fragment.familyAdapter.itemCount

                fragment.binding.horizontalRecyclerView.smoothScrollToPosition(nextPosition)
                fragment.autoScrollHandler.postDelayed(this, 5000)
            }
        }
    }

}