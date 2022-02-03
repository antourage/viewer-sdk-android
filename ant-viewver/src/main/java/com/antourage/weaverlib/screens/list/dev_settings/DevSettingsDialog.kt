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
import androidx.core.content.ContextCompat
import com.antourage.weaverlib.BuildConfig
import com.antourage.weaverlib.ConfigManager
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import kotlinx.android.synthetic.main.dialog_backend_choice.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.textColor

//TODO make internal
class DevSettingsDialog(
    context: Context,
    private val listener: OnDevSettingsChangedListener
) :
    Dialog(context) {

    companion object{
       const val PROD = "prod"
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_backend_choice)
        if (!ConfigManager.configFile.environments.isNullOrEmpty()) {
            populateEnvs()
            setTxt.setOnClickListener {
                val radioButton = rg_links.findViewById<RadioButton>(rg_links.checkedRadioButtonId)
                if (radioButton.text.toString() != UserCache.getInstance()?.getEnvChoice()) {
                    GlobalScope.launch(Dispatchers.IO) {
                        UserCache.getInstance()?.clearUserData()
                    }
                }
                listener.onBeChanged(radioButton.text.toString())
                this.dismiss()
            }
        } else {
            tv_title_dialog.visibility = View.GONE
            rg_links.visibility = View.GONE
            setTxt.visibility = View.GONE
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

    private fun populateEnvs() {
        ConfigManager.configFile.environments?.let {
            it.forEach { env ->
                val rb = RadioButton(context)
                rb.text = env.name
                rb.textColor = ContextCompat.getColor(context, R.color.ant_white)
                rg_links.addView(rb)
            }
        }
        val rb = RadioButton(context)
        rb.text = PROD
        rb.textColor = ContextCompat.getColor(context, R.color.ant_white)
        rg_links.addView(rb)

        initBECheckedBtn()
    }

    private fun initBECheckedBtn() {
        val count: Int = rg_links.childCount
        for (i in 0 until count) {
            val o: View = rg_links.getChildAt(i)
            if (o is RadioButton) {
                o.isChecked = o.text == UserCache.getInstance()?.getEnvChoice()
            }
        }
    }
}
