package org.sabda.family.utility

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.appcompat.app.AlertDialog

object NetworkUtil {

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun showNoInternetDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Masalah Koneksi Internet")
            .setMessage("Silakan sambungkan perangkat dengan internet untuk memulai aplikasi ini")
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->
                if (!isInternetAvailable(context)) {
                    if (context is Activity) {
                        context.finishAffinity()
                    }
                } else {
                    dialog.dismiss()
                }
            }
            .show()
    }
}