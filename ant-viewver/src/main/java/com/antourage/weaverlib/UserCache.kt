package com.antourage.weaverlib

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.antourage.weaverlib.dev_settings.DevSettingsDialog
import java.lang.ref.WeakReference

class UserCache private constructor(context: Context) {
    private var contextRef: WeakReference<Context>? = null
    private var prefs: SharedPreferences? = null

    init {
        this.contextRef = WeakReference(context)
        this.prefs = contextRef?.get()?.getSharedPreferences(ANT_PREF, MODE_PRIVATE)
    }

    companion object {
        private const val ANT_PREF = "ant_pref"
        private const val SP_ENV_CHOICE = "sp_env_choice"

        private var INSTANCE: UserCache? = null

        @Synchronized
        fun getInstance(context: Context): UserCache? {
            if (INSTANCE == null) {
                INSTANCE = UserCache(context)
            }
            return INSTANCE
        }

        @Synchronized
        fun getInstance(): UserCache? {
            return INSTANCE
        }
    }

    fun getEnvChoice(): String {
        return prefs?.getString(SP_ENV_CHOICE, null) ?: DevSettingsDialog.PROD
    }

    fun updateEnvChoice(env: String) {
        prefs?.edit()
            ?.putString(SP_ENV_CHOICE, env)
            ?.apply()
    }
}