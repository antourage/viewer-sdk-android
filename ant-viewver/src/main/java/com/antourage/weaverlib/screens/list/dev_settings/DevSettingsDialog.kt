package com.antourage.weaverlib.screens.list.dev_settings

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.widget.RadioButton
import com.antourage.weaverlib.BuildConfig
import com.antourage.weaverlib.PropertyManager
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.isAppInstalledFromGooglePlay
import com.antourage.weaverlib.other.room.AppDatabase
import kotlinx.android.synthetic.main.dialog_backend_choice.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


internal class DevSettingsDialog(
    context: Context,
    private val listener: OnDevSettingsChangedListener
) :
    Dialog(context) {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_backend_choice)
        if (!isAppInstalledFromGooglePlay(context)) {
            initBECheckedBtn()
            val baseUrl = PropertyManager.getInstance()?.getProperty(PropertyManager.BASE_URL)
            rb_dev.text = "dev: ${EnvironmentManager.getUrlForEnv(baseUrl,Environments.DEV)}"
            rb_load.text = "load: ${EnvironmentManager.getUrlForEnv(baseUrl,Environments.LOAD_STAGING)}"
            rb_staging.text = "stage: ${EnvironmentManager.getUrlForEnv(baseUrl,Environments.STAGING)}"
            rb_demo.text = "demo: ${EnvironmentManager.getUrlForEnv(baseUrl,Environments.DEMO)}"
            rb_prod.text = "prod: ${EnvironmentManager.getUrlForEnv(baseUrl,Environments.PROD)}"
            setTxt.setOnClickListener {
                val radioButton = rg_links.findViewById<RadioButton>(rg_links.checkedRadioButtonId)
                val backEndUrl = when {
                    radioButton.text.contains("dev") -> Environments.DEV
                    radioButton.text.contains("load") -> Environments.LOAD_STAGING
                    radioButton.text.contains("stage") -> Environments.STAGING
                    radioButton.text.contains("demo") -> Environments.DEMO
                    radioButton.text.contains("prod") -> Environments.PROD
                    else -> Environments.PROD
                }
                if (backEndUrl != EnvironmentManager.currentEnv) {
                    GlobalScope.launch(Dispatchers.IO) {
                        AppDatabase.getInstance(context).commentDao().clearComments()
                        AppDatabase.getInstance(context).videoStopTimeDao().clearVideos()
                    }
                }
                listener.onBeChanged(backEndUrl)
                this.dismiss()
            }
        } else {
            tv_title_dialog.visibility = View.GONE
            rg_links.visibility = View.GONE
            setTxt.visibility = View.GONE
        }

        btnLogout.setOnClickListener {
            UserCache.getInstance()?.logout()
            dismiss()
        }

        val versionName = BuildConfig.VERSION_NAME
        txtModuleVersion.text = context.resources.getString(R.string.ant_version_name, versionName)

        /*
        handler is used here to prevent immediate dialog closing in case user made more clicks
         than needed to open it.
         */
        setCanceledOnTouchOutside(false)
        Handler(Looper.getMainLooper()).postDelayed({
            setCanceledOnTouchOutside(true)
        }, 1500)
    }

    private fun initBECheckedBtn() {
        val radioButton: RadioButton? = when (EnvironmentManager.currentEnv) {
            Environments.DEV -> findViewById(R.id.rb_dev)
            Environments.LOAD_STAGING -> findViewById(R.id.rb_load)
            Environments.STAGING -> findViewById(R.id.rb_staging)
            Environments.DEMO -> findViewById(R.id.rb_demo)
            Environments.PROD -> findViewById(R.id.rb_prod)
        }
        if (radioButton != null)
            radioButton.isChecked = true
    }
}
