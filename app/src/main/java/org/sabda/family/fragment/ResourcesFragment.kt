package org.sabda.family.fragment

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sabda.family.R
import org.sabda.family.ResourcesActivity
import org.sabda.family.adapter.FamilyAdapter
import org.sabda.family.base.BaseFragment
import org.sabda.family.data.repository.FamilyRepository
import org.sabda.family.data.viewmodel.SharedViewModel
import org.sabda.family.databinding.FragmentResourcesBinding
import org.sabda.family.model.FamilyData
import org.sabda.family.utility.LoadingUtil
import org.sabda.family.utility.MenuUtil
import org.sabda.family.utility.NetworkUtil

class ResourcesFragment : BaseFragment<FragmentResourcesBinding>() {

    private lateinit var familyAdapter: FamilyAdapter
    private val familyList = mutableListOf<FamilyData>()
    private val familyRepository by lazy { FamilyRepository(requireContext()) }
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private var isFilterApplied = false

    private lateinit var noResultsText: TextView
    private lateinit var loadingTextView: TextView
    private val loadingUtil = LoadingUtil()

    private val isNightMode: Boolean
        get() = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentResourcesBinding.inflate(inflater, container, false)

    override fun onViewCreated( view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkInternetAndProceed {
            setupRecyclerView()
            setupNoResultsText()
            setupLoadingTextView()
            fetchFamilyData()
            setupButtons()
            setupSearchView()
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
        familyAdapter = FamilyAdapter { familyId ->
            val intent = Intent(context, ResourcesActivity::class.java).apply {
                putExtra("familyId", familyId)
                Log.d("checkfamilyid", "setupRecyclerView: $familyId ")
            }
            startActivity(intent)
        }
        binding.recyclerView2.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = familyAdapter
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

    private fun setupNoResultsText() {
        noResultsText = TextView(context).apply {
            text = getString(R.string.no_results)
            textSize = 18f
            setTextColor(resources.getColor(android.R.color.black, null))
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER

            }
            visibility = View.GONE
        }

        (binding.root as ViewGroup).addView(noResultsText)
    }

    private fun fetchFamilyData() {
        checkInternetAndProceed {
            showLoading()
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val familyDataList = withContext(Dispatchers.IO) {
                        familyRepository.fetchFamilyData(fragmentName = "ResourcesFragment")
                    }
                    sharedViewModel.setResourcesFamilyData(familyList)
                    familyList.clear()
                    familyList.addAll(familyDataList.shuffled())

                    familyAdapter.updateData(familyList)
                } catch (e: Exception) {
                    Log.e("List_Data", "Error fetching family data", e)
                } finally {
                    hideLoading()
                }
            }
        }
    }

    private fun setupButtons() {
        binding.filterImage.setOnClickListener { toggleFilter() }
    }

    private fun toggleFilter() {
        if (isFilterApplied) {
            clearFilter()
        } else {
            context?.let { context ->
                MenuUtil(context).setupFilterMenu(binding.filterImage, this) {
                    isFilterApplied = false // Reset status filter saat menu ditutup
                    updateFilterButton() // Perbarui ikon filter
                }
            }
            isFilterApplied = true
            updateFilterButton()
        }
    }

    private fun clearFilter() {
        fetchFamilyData()
        isFilterApplied = false
        updateFilterButton()
    }

    private fun updateFilterButton() {
        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val iconRes = if (isFilterApplied) {
            if (isNightMode) R.drawable.baseline_filter_white_off_24 else R.drawable.baseline_filter_alt_off_24
        } else {
            if (isNightMode) R.drawable.baseline_filter_white else R.drawable.baseline_filter_list_alt_24
        }

        binding.filterImage.setImageResource(iconRes)
    }

    // Pengaturan ambil data dari filtered API -> cek repository/FamilyRepository.kt
    fun applyFilter(itemTitle: String, query: String) {
        lifecycleScope.launch {
            try {
                val filteredData = withContext(Dispatchers.IO) {
                    familyRepository.fetchFamilyData(itemTitle = itemTitle, query =  query)
                }
                familyAdapter.updateData(filteredData)
            } catch (e: Exception) { Log.e("ListDataActivity", "Error applying filter", e) }
        }
    }

    // Pengaturan SearchView
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { filterFamilyList(it) }
                return true
            }
        })
    }

    // Pengaturan Searchview untuk parameternya
    private fun filterFamilyList(query: String) {
        val filteredList = familyList.filter {
            it.title.contains(query, ignoreCase = true)
        }

        if (filteredList.isEmpty()) {
            noResultsText.visibility = View.VISIBLE
            binding.recyclerView2.visibility = View.GONE
        } else {
            noResultsText.visibility = View.GONE
            binding.recyclerView2.visibility = View.VISIBLE
        }

        familyAdapter.updateData(filteredList)

    }

    private fun setViewsVisibility(visibility: Int) {
        if (!isAdded) return
        binding.apply {
            recyclerView2.visibility = visibility
            filterImage.visibility = visibility
            searchView.visibility = visibility
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
}


