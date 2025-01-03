package org.sabda.family.fragment

import android.app.AlertDialog
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sabda.family.R
import org.sabda.family.data.repository.RenunganRepository
import org.sabda.family.data.viewmodel.SharedViewModel
import org.sabda.family.databinding.FragmentRenunganBinding
import org.sabda.family.model.RenunganData
import org.sabda.family.utility.HtmlUtil
import org.sabda.family.utility.LoadingUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RenunganFragment : Fragment() {

    private val renunganRepository = RenunganRepository()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var currentRenunganData: RenunganData? = null
    private var currentDate: Calendar = Calendar.getInstance()

    private lateinit var binding: FragmentRenunganBinding

    private lateinit var loadingTextView: TextView
    private val loadingUtil = LoadingUtil()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRenunganBinding.inflate(inflater, container, false)

        setupButtons()
        setupLoadingTextView()
        fetchDisplayRenungan()
        observeRenunganData()

        return binding.root
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

    private fun setupButtons() {
        binding.verseButton.setOnClickListener { fetchDialogVerse() }
        binding.natsButton.setOnClickListener { fetchDialogNats() }
        binding.tanggalButton.setOnClickListener { showDatePicker() }
        binding.prevButton.setOnClickListener { changeDate(-1) }
        binding.nextButton.setOnClickListener { changeDate(1) }
    }

    private fun fetchDialogVerse() {
        val ayat = currentRenunganData?.verse_1

        lifecycleScope.launch {
            val verseText = withContext(Dispatchers.IO){
                ayat?.let { renunganRepository.fetchVerseTexts(it) }
            }

            val message = verseText?.entries?.joinToString("\n\n") { (reference, text) ->
                "$reference:\n$text\n"
            } ?: "Tidak ada teks yang ditemukan."

            AlertDialog.Builder(context)
                .setTitle("Ayat: $ayat")
                .setMessage(HtmlUtil.removeHtmlTags(message))
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    private fun fetchDialogNats() {
        val ayat = currentRenunganData?.nats_verse

        Log.d("Cek ayat Nats", "fetchDialogNats: $ayat")

        lifecycleScope.launch {
            val natsText = withContext(Dispatchers.IO){
                ayat?.let { renunganRepository.fetchAllNatsTexts(it) }
            }

            val message = natsText?.entries?.joinToString("\n\n") { (version, text) ->
                "$version: $text"
            } ?: "Tidak ada teks yang ditemukan."

            AlertDialog.Builder(context)
                .setTitle("Ayat Nats: $ayat")
                .setMessage(HtmlUtil.removeHtmlTags(message))
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    private fun fetchDisplayRenungan() {
        if (sharedViewModel.hasData()) {
            Log.d("RenunganFragment", "Data sudah dimuat, tidak memuat ulang.")
            sharedViewModel.renunganData.value?.let { displayRenungan(it) }
            return
        } else {
            showLoading()
            lifecycleScope.launch {
                try {
                    val renungan = withContext(Dispatchers.IO) {
                        renunganRepository.fetchRenungan()
                    }

                    Log.d("Repository", "Data diambil: $renungan")

                    renungan?.let {
                        sharedViewModel.setRenunganData(renungan)
                        displayRenungan(renungan)
                    }
                } finally {
                    hideLoading()
                }
            }
        }
    }

    private fun fetchRenunganBySelectedDate(date: String) {
        lifecycleScope.launch {
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
            content.text        = HtmlUtil.removeRenunganTags(renunganData.content)
            footer.text         = HtmlUtil.removeRenunganTags(renunganData.footer)
            verseButton.text    = String.format("Bacaan: %s", renunganData.verse_1)
            natsButton.text     = renunganData.nats_verse
            tanggalButton.text  = renunganData.date
        }
    }

    private fun changeDate(dayOffset: Int) {
        currentDate.add(Calendar.DAY_OF_MONTH, dayOffset)

        val dateFormatApi = SimpleDateFormat("ddMM", Locale("id", "ID"))
        val formattedDateApi = dateFormatApi.format(currentDate.time)

        val dateFormatDisplay = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        val formattedDateDisplay = dateFormatDisplay.format(currentDate.time)

        binding.tanggalButton.text = formattedDateDisplay
        fetchRenunganBySelectedDate(formattedDateApi)
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