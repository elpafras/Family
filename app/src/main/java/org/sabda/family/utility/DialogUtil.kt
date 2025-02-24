package org.sabda.family.utility

import android.app.AlertDialog
import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sabda.family.data.repository.RenunganRepository

object DialogUtil {

    fun fetchDialogVerse(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        renunganRepository: RenunganRepository,
        verse: String?
    ) {
        lifecycleScope.launch {
            val verseText = withContext(Dispatchers.IO) {
                verse?.let { renunganRepository.fetchVerseTexts(it) }
            }

            val message = verseText?.entries?.joinToString("\n\n") { (reference, text) ->
                "$reference:$text"
            } ?: "Tidak ada teks yang ditemukan"

            AlertDialog.Builder(context)
                .setTitle("Ayat: $verse")
                .setMessage(HtmlUtil.removeHtmlTags(message))
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    fun fetchDialogNats(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        renunganRepository: RenunganRepository,
        natsVerse: String?
    ) {
        lifecycleScope.launch {
            val natsText = withContext(Dispatchers.IO) {
                natsVerse?.let { renunganRepository.fetchAllNatsTexts(it) }
            }

            val message = natsText?.entries?.joinToString("\n\n") { (version, text) ->
                "${version.uppercase()}: $text"
            } ?: "Tidak ada teks yang ditemukan."

            AlertDialog.Builder(context)
                .setTitle("Ayat Nats: $natsVerse")
                .setMessage(HtmlUtil.removeHtmlTags(message))
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }
}