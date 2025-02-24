package org.sabda.family.fragment

import android.app.DatePickerDialog
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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sabda.family.base.BaseFragment
import org.sabda.family.data.repository.RenunganRepository
import org.sabda.family.data.viewmodel.SharedViewModel
import org.sabda.family.databinding.FragmentRenunganBinding
import org.sabda.family.model.RenunganData
import org.sabda.family.utility.DialogUtil
import org.sabda.family.utility.HtmlUtil
import org.sabda.family.utility.LoadingUtil
import org.sabda.family.utility.NetworkUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RenunganFragment : BaseFragment<FragmentRenunganBinding>() {

    private lateinit var renunganRepository: RenunganRepository
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var currentRenunganData: RenunganData? = null
    private var currentDate: Calendar = Calendar.getInstance()

    private lateinit var loadingTextView: TextView
    private val loadingUtil = LoadingUtil()

    private val isNightMode: Boolean
        get() = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentRenunganBinding.inflate(inflater, container, false)

    override fun onBackPressed() {
        // Misalnya tampilkan konfirmasi sebelum keluar dari aplikasi
        Toast.makeText(requireContext(), "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        renunganRepository = RenunganRepository(requireContext())

        if (!NetworkUtil.isInternetAvailable(requireContext())) {
            NetworkUtil.showNoInternetDialog(requireContext())
            return
        }

        currentDate = sharedViewModel.currentDate.value ?: Calendar.getInstance().also {
            sharedViewModel.setCurrentDate(it)
        }

        setupButtons()
        setupLoadingTextView()
        fetchDisplayRenungan()
        observeRenunganData()

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

    private fun setupButtons() {

        binding.verseButton.setOnClickListener {
            DialogUtil.fetchDialogVerse(
                requireContext(),
                lifecycleScope,
                renunganRepository,
                currentRenunganData?.verse_1
            )
        }
        binding.natsButton.setOnClickListener {
            DialogUtil.fetchDialogNats(
                requireContext(),
                lifecycleScope,
                renunganRepository,
                currentRenunganData?.nats_verse
            )
        }
        binding.tanggalButton.setOnClickListener { showDatePicker() }
        binding.prevButton.setOnClickListener { changeDate(-1) }
        binding.nextButton.setOnClickListener { changeDate(1) }
    }



    private fun fetchDisplayRenungan() {
        if (sharedViewModel.hasData()) {
            binding.tanggalButton.text = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                .format(currentDate.time)

            Log.d("RenunganFragment", "Data sudah dimuat, tidak memuat ulang.")
            sharedViewModel.renunganData.value?.let(::displayRenungan)
        } else {
            showLoading()
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val renungan = withContext(Dispatchers.IO) {
                        renunganRepository.fetchRenungan()
                    }

                    renungan?.let {
                        sharedViewModel.setRenunganData(renungan)
                        displayRenungan(renungan)
                    }
                } finally {
                    if (isAdded) hideLoading()
                }
            }
        }
    }

    private fun fetchRenunganBySelectedDate(date: String) {
        if (!NetworkUtil.isInternetAvailable(requireContext())) {
            NetworkUtil.showNoInternetDialog(requireContext())
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val renungan = withContext(Dispatchers.IO) {
                renunganRepository.fetchRenunganByDate(date)
            }

            renungan?.let {
                sharedViewModel.setRenunganData(renungan)
                displayRenungan(renungan)
            }
        }
    }

    private fun displayRenungan(renunganData: RenunganData) {
        if (currentRenunganData == renunganData) {
            Log.d("RenunganFragment", "Data sudah ditampilkan, tidak memperbarui UI.")
            return
        }

        currentRenunganData     = renunganData
        with(binding) {
            titleRenungan.text  = renunganData.title
            nats.text           = renunganData.nats
            content.text        = HtmlUtil.removeSpesificTags(renunganData.content)
            footer.text         = HtmlUtil.removeSpesificTags(renunganData.footer)
            verseButton.text    = String.format("Bacaan: %s", renunganData.verse_1)
            natsButton.text     = renunganData.nats_verse
            tanggalButton.text  = renunganData.date
        }
    }

    private fun changeDate(dayOffset: Int) {
        currentDate.apply {
            add(Calendar.DAY_OF_MONTH, dayOffset)
            sharedViewModel.setCurrentDate(this)
            binding.tanggalButton.text = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(time)
            fetchRenunganBySelectedDate(SimpleDateFormat("ddMM", Locale("id", "ID")).format(time))
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        context?.let {
            DatePickerDialog(it, { _, selectedYear, selectedMonth, selectedDaOfMonth ->
                    val selectedDate = Calendar.getInstance().apply {
                        set(selectedYear, selectedMonth, selectedDaOfMonth)
                    }

                    val dateFormatApi = SimpleDateFormat("ddMM", Locale("id", "ID"))
                    val formattedDateApi = dateFormatApi.format(selectedDate.time)

                    val dateFormatDisplay = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                    val formattedDateDisplay = dateFormatDisplay.format(selectedDate.time)

                    binding.tanggalButton.text = formattedDateDisplay

                    Log.d("fmtAPI", "showDatePicker: $formattedDateApi")
                    fetchRenunganBySelectedDate(formattedDateApi)
                }, year, month, dayOfMonth)
        }?.show()
    }

    private fun observeRenunganData() {
        sharedViewModel.renunganData.observe(viewLifecycleOwner) { renunganData ->
            Log.d("SharedViewModel", "observeRenunganData: $renunganData")
            renunganData?.let { displayRenungan(it) }
        }
    }

    private fun setViewsVisibility(visibility: Int) {
        if (!isAdded) return
        binding.apply {
            verseButton.visibility = visibility
            natsButton.visibility = visibility
            tanggalButton.visibility = visibility
            nextButton.visibility = visibility
            prevButton.visibility = visibility
            titleRenungan.visibility = visibility
            nats.visibility = visibility
            content.visibility = visibility
            footer.visibility = visibility
            sumber.visibility = visibility
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