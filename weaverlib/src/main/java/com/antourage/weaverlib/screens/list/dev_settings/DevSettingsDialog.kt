package com.antourage.weaverlib.screens.list.dev_settings

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.RadioButton
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache

import kotlinx.android.synthetic.main.dialog_bechoice.*

class DevSettingsDialog(context: Context, private val listener: OnDevSettingsChangedListener) : Dialog(context) {

    companion object{
        const val BASE_URL_LOCAL = "http://192.168.20.196:3000/"
        const val BASE_URL_DEV = "https://api-myra.net/"
        const val BASE_URL_STAGING = "https://staging-myra.com/"
        const val BASE_URL_PROD = "https://myra.technology/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_bechoice)
        initBECheckedBtn(UserCache.newInstance().getBeChoice(context.applicationContext))
        rb_dev.text = BASE_URL_DEV
        rb_staging.text = BASE_URL_STAGING
        rb_production.text = BASE_URL_PROD
        rb_local.text = BASE_URL_LOCAL
        setTxt.setOnClickListener { v ->
            val radioButton = rg_links.findViewById<RadioButton>(rg_links.checkedRadioButtonId)
            listener.onBeChanged(radioButton.text.toString())
            this.dismiss()
        }
    }

    private fun initBECheckedBtn(beChoice: String?) {
        val radioButton: RadioButton?
        when (beChoice) {
            BASE_URL_LOCAL -> radioButton = findViewById(R.id.rb_local)
            BASE_URL_DEV -> radioButton = findViewById(R.id.rb_dev)
            BASE_URL_STAGING -> radioButton = findViewById(R.id.rb_staging)
            BASE_URL_PROD -> radioButton = findViewById(R.id.rb_production)
            else -> radioButton = findViewById(R.id.rb_production)
        }
        if (radioButton != null)
            radioButton.isChecked = true
    }


}
