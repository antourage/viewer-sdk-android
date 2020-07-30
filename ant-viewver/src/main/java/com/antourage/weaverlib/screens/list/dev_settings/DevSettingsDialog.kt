package com.antourage.weaverlib.screens.list.dev_settings

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.RadioButton
import com.antourage.weaverlib.BuildConfig
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import kotlinx.android.synthetic.main.dialog_backend_choice.*


internal class DevSettingsDialog(
    context: Context,
    private val listener: OnDevSettingsChangedListener
) :
    Dialog(context) {

    companion object {
        const val BASE_URL_DEV = "https://api.dev.antourage.com/"
        const val BASE_URL_LOAD = "https://api.load-staging.antourage.com/"
        const val BASE_URL_STAGING = "https://api.staging.antourage.com/"
        const val BASE_URL_DEMO = "https://api.demo.antourage.com/"
        const val BASE_URL_PROD = "https://api.antourage.com/"
        const val DEFAULT_URL = BASE_URL_PROD
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_backend_choice)
        initBECheckedBtn(UserCache.getInstance(context.applicationContext)?.getBeChoice())
        rb_dev.text = "dev: $BASE_URL_DEV"
        rb_load.text = "load: $BASE_URL_LOAD"
        rb_staging.text = "stage: $BASE_URL_STAGING"
        rb_demo.text = "demo: $BASE_URL_DEMO"
        rb_prod.text = "prod: $BASE_URL_PROD"
        setTxt.setOnClickListener { _ ->
            val radioButton = rg_links.findViewById<RadioButton>(rg_links.checkedRadioButtonId)
            val backEndUrl = when {
                radioButton.text.contains("dev") -> BASE_URL_DEV
                radioButton.text.contains("load") -> BASE_URL_LOAD
                radioButton.text.contains("stage") -> BASE_URL_STAGING
                radioButton.text.contains("demo") -> BASE_URL_DEMO
                radioButton.text.contains("prod") -> BASE_URL_PROD
                else -> BASE_URL_PROD
            }
            listener.onBeChanged(backEndUrl)
            this.dismiss()
        }
        setCanceledOnTouchOutside(false)

        val versionName = BuildConfig.VERSION_NAME
        txtModuleVersion.text = context.resources.getString(R.string.ant_version_name, versionName)
    }

    private fun initBECheckedBtn(beChoice: String?) {
        val radioButton: RadioButton? = when (beChoice) {
            BASE_URL_DEV -> findViewById(R.id.rb_dev)
            BASE_URL_LOAD -> findViewById(R.id.rb_load)
            BASE_URL_STAGING -> findViewById(R.id.rb_staging)
            BASE_URL_DEMO -> findViewById(R.id.rb_demo)
            BASE_URL_PROD -> findViewById(R.id.rb_prod)
            else -> findViewById(R.id.rb_prod)
        }
        if (radioButton != null)
            radioButton.isChecked = true
    }
}
