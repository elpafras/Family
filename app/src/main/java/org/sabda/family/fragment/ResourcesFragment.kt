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
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sabda.family.DetailActivity
import org.sabda.family.R
import org.sabda.family.adapter.FamilyAdapter
import org.sabda.family.data.repository.FamilyRepository
import org.sabda.family.data.viewmodel.SharedViewModel
import org.sabda.family.databinding.FragmentResourcesBinding
import org.sabda.family.model.FamilyData
import org.sabda.family.utility.LoadingUtil
import org.sabda.family.utility.MenuUtil

class ResourcesFragment : Fragment() {

    private lateinit var binding: FragmentResourcesBinding
    private lateinit var familyAdapter: FamilyAdapter
    private val familyList = mutableListOf<FamilyData>()
    private val familyRepository = FamilyRepository()
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private var isFilterApplied = false

    private lateinit var loadingTextView: TextView
    private val loadingUtil = LoadingUtil()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentResourcesBinding.inflate(inflater, container, false)

        setupRecyclerView()
        setupLoadingTextView()
        fetchFamilyData()
        setupButtons()
        setupSearchView()

        return binding.root
    }


    private fun setupRecyclerView() {
        familyAdapter = FamilyAdapter(familyList) { familyId ->
            val intent = Intent(context, DetailActivity::class.java).apply {
                putExtra("familyId", familyId)
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

                val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
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

    private fun fetchFamilyData() {
        if (sharedViewModel.hasFamilyData()) {
            familyList.clear()
            familyList.addAll(sharedViewModel.familyData.value!!)
            familyAdapter.updateData(familyList)
            familyAdapter.notifyDataSetChanged()
            return
        } else {
            showLoading()
            lifecycleScope.launch {
                try {
                    val familyDataList = withContext(Dispatchers.IO) {
                        familyRepository.fetchFamilyData()
                    }
                    sharedViewModel.setFamilyData(familyDataList)
                    familyList.clear()
                    familyList.addAll(familyDataList)
                    familyAdapter.updateData(familyList)
                    familyAdapter.notifyDataSetChanged()
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
            context?.let { MenuUtil(it).setupFilterMenu(binding.filterImage, this) }
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
                    familyRepository.fetchFilteredFamilyData(itemTitle, query)
                }
                familyAdapter.updateData(filteredData)
            } catch (e: Exception) { Log.e("ListDataActivity", "Error applying filter", e) }
        }
    }

    // Pengaturan SearchView
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { filterFamilyList(it) }
                return true
            }
        })
    }

    // Pengaturan Searchview untuk parameternya
    private fun filterFamilyList(query: String) {
        val filteredList = familyList.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.author.contains(query, ignoreCase = true)
        }
        familyAdapter.updateData(filteredList)
    }

    private fun setViewsVisibility(visibility: Int) {
        binding.apply {
            recyclerView2.visibility = visibility
            filterImage.visibility = visibility
            searchView.visibility = visibility
        }
    }

    private fun showLoading() {
        loadingUtil.showLoadingWebView(loadingTextView)
        setViewsVisibility(View.GONE)
    }

    private fun hideLoading() {
        loadingUtil.hideLoadingWebView(loadingTextView)
        setViewsVisibility(View.VISIBLE)
    }
}