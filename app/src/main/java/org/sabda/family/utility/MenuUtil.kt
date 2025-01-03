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

    private lateinit var popupMenu: PopupMenu

    fun setupMenu(view: View, currentActivity: String) {
        popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.menu_option)

        when (currentActivity) {
            "AboutActivity" -> popupMenu.menu.findItem(R.id.about).isVisible = false
        }

        popupMenu.setOnMenuItemClickListener { item ->
            handleMenuSelection(item)
            true
        }

        popupMenu.show()
    }

    private fun handleMenuSelection(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.about -> context.startActivity(Intent(context, AboutActivity::class.java))
        }
    }

    fun setupFilterMenu(view: View, fragment: Fragment) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.filter_menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.category   -> fetchAndPopulateFilterMenu("categories", view, fragment)
                R.id.series     -> fetchAndPopulateFilterMenu("series", view, fragment)
                R.id.media      -> fetchAndPopulateFilterMenu("media_type", view, fragment)
            }
            true
        }

        popupMenu.show()
    }

    private fun fetchAndPopulateFilterMenu(query: String, view: View, fragment: Fragment) {
        val apiUrl = "https://dev.sabda.org/unhack/2024/api/family/getTaxonomies.php?q=$query"
        val url = URL(apiUrl)

        Thread {
            var connection: HttpURLConnection? = null
            try {
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.use { it.readText() }
                    val options = parseOptions(response)

                    (context as? Activity)?.runOnUiThread { showFilterMenu(options, view, query, fragment) }

                }
            } catch (e: Exception) {
                Log.e("MenuUtil", "Error fetching data ", e)
                Toast.makeText(context, "Error fetching data", Toast.LENGTH_SHORT).show()
            } finally {
                connection?.disconnect()
            }
        }.start()
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
}