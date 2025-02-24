package org.sabda.family

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.sabda.family.utility.NetworkUtil
import org.sabda.family.utility.StatusBarUtil

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        StatusBarUtil().setLightStatusBar(this, R.color.white)

        if (NetworkUtil.isInternetAvailable(this)) {
            startActivity(Intent(this, DashboardActivity::class.java))
        } else{
            NetworkUtil.showNoInternetDialog(this)
        }
    }
}