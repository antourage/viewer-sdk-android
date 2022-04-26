package com.antourage.weaverlib.dev_settings

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.RadioButton
import androidx.core.content.ContextCompat
import com.antourage.weaverlib.BuildConfig
import com.antourage.weaverlib.ConfigManager
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.models.SubscribeToPushesRequest
import com.antourage.weaverlib.other.networking.push.PushRepository
import com.antourage.weaverlib.ui.fab.AntourageFab.Companion.cachedFcmToken
import com.antourage.weaverlib.ui.fab.AntourageFab.Companion.teamId
import kotlinx.android.synthetic.main.dialog_backend_choice.*
import org.jetbrains.anko.textColor

class DevSettingsDialog(
    context: Context,
    private val listener: OnDevSettingsChangedListener
) :
    Dialog(context) {

    companion object {
        const val PROD = "prod"
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_backend_choice)
        if (ConfigManager.isConfigInitialized() && !ConfigManager.configFile.environments.isNullOrEmpty()) {
            populateEnvs()
            setTxt.setOnClickListener {
                val radioButton = rg_links.findViewById<RadioButton>(rg_links.checkedRadioButtonId)
                if (radioButton.text.toString() != UserCache.getInstance()?.getEnvChoice()) {
                    PushRepository.unsubscribeFromPushNotifications(
                        SubscribeToPushesRequest(
                            cachedFcmToken,
                            teamId
                        )
                    )
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
        setCanceledOnTouchOutside(true)
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
