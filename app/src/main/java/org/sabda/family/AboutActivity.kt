package org.sabda.family

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.sabda.family.databinding.ActivityAboutBinding
import org.sabda.family.utility.StatusBarUtil

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        StatusBarUtil().setLightStatusBar(this, R.color.white)
        setupButtons()
    }

    private fun setupButtons() {
        binding.back.setOnClickListener { startActivity(Intent(this, DashboardActivity::class.java)) }
    }
}