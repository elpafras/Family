package org.sabda.family

import android.content.Intent
import android.os.Bundle
import org.sabda.family.base.BaseActivity
import org.sabda.family.databinding.ActivityAboutBinding
import org.sabda.family.utility.StatusBarUtil

class AboutActivity : BaseActivity<ActivityAboutBinding>() {

    override fun setupViewBinding(): ActivityAboutBinding {
        return ActivityAboutBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StatusBarUtil().setLightStatusBar(this, R.color.white)
        setupButtons()
    }

    private fun setupButtons() {
        binding.back.setOnClickListener { startActivity(Intent(this, DashboardActivity::class.java)) }
    }
}