package com.antourage.weaverlib.screens.list.dev_settings

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.RadioButton
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache

import kotlinx.android.synthetic.main.dialog_backend_choice.*

class DevSettingsDialog(context: Context, private val listener: OnDevSettingsChangedListener) :
    Dialog(context) {

    companion object {
        const val BASE_URL_DEV = "http://35.156.199.125/"
        const val BASE_URL_STAGING = "http://3.124.42.114/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_backend_choice)
        initBECheckedBtn(UserCache.getInstance(context.applicationContext)?.getBeChoice())
        rb_dev.text = BASE_URL_DEV
        rb_staging.text = BASE_URL_STAGING
        setTxt.setOnClickListener { v ->
            val radioButton = rg_links.findViewById<RadioButton>(rg_links.checkedRadioButtonId)
            listener.onBeChanged(radioButton.text.toString())
            this.dismiss()
        }
        setCanceledOnTouchOutside(false)
    }

    private fun initBECheckedBtn(beChoice: String?) {
        val radioButton: RadioButton? = when (beChoice) {
            BASE_URL_DEV -> findViewById(R.id.rb_dev)
            BASE_URL_STAGING -> findViewById(R.id.rb_staging)
            else -> findViewById(R.id.rb_staging)
        }
        if (radioButton != null)
            radioButton.isChecked = true
    }
}
