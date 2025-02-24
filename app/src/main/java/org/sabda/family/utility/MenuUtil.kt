package org.sabda.family.utility

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.json.JSONObject
import org.sabda.family.AboutActivity
import org.sabda.family.R
import org.sabda.family.fragment.ResourcesFragment
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MenuUtil(private val context: Context) {

    fun setupMenu(view: View, currentActivity: String) {
        createPopMenu(view, R.menu.menu_option).apply {
            if (currentActivity == "AboutActivity") {
                menu.findItem(R.id.about).isVisible = false
            }
            setOnMenuItemClickListener { handleMenuSelection(it) }
            show()
        }
    }

    fun setupFilterMenu(view: View, fragment: Fragment, onCloseMenu: () -> Unit = {}) {
        createPopMenu(view, R.menu.filter_menu).apply {
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.category -> fetchAndPopulateFilterMenu("categories", view, fragment)
                    R.id.series -> fetchAndPopulateFilterMenu("series", view, fragment)
                    R.id.media -> fetchAndPopulateFilterMenu("media_type", view, fragment)
                }
                true
            }
            setOnDismissListener { onCloseMenu() }
            show()
        }
    }

    private fun createPopMenu(anchor: View, menuRes: Int): PopupMenu {
        return PopupMenu(context, anchor).apply {
            inflate(menuRes)
        }
    }

    private fun handleMenuSelection(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.about -> {
                context.startActivity(Intent(context, AboutActivity::class.java))
                true
            }
            else -> false
        }
    }

    private fun fetchAndPopulateFilterMenu(query: String, view: View, fragment: Fragment) {
        val apiUrl = context.getString(R.string.populate_url, query)
        Thread {
            val options = fetchDataFromApi(apiUrl)
            (context as? Activity)?.runOnUiThread {
                showFilterMenu(options, view, query, fragment)
            }
        }.start()
    }

    private fun fetchDataFromApi(apiUrl: String): List<String> {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    parseOptions(reader.readText())
                }
            } else {
                showError("Failed to fetch data")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("MenuUtil", "Error fetching data", e)
            showError("Error fetching data")
            emptyList()
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseOptions(data: String): List<String> {
        val options = mutableListOf<String>()
        try {
            val jsonObject = JSONObject(data)

            jsonObject.keys().forEach { key ->
                val itemObject = jsonObject.getJSONObject(key)
                val name = itemObject.getString("name")
                options.add(name)
            }
        } catch (e: Exception){
            Log.e("MenuUtil", "Failed to parse response", e)
        }
        return options
    }

    private fun showFilterMenu(options: List<String>, anchorView: View, query: String, fragment: Fragment) {
        val filterPopUpMenu = PopupMenu(context, anchorView)

        options.forEachIndexed { index, s -> filterPopUpMenu.menu.add(0, index, 0, s) }

        filterPopUpMenu.setOnMenuItemClickListener { item ->

            Toast.makeText(context, "selected: ${item.title} + $query", Toast.LENGTH_SHORT).show()

            if (fragment is ResourcesFragment) {
                fragment.applyFilter(item.title.toString(), query)
            }

            true

        }

        filterPopUpMenu.show()
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}