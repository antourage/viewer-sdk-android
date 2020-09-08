package com.antourage.weaverlib.other

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.*

/** class for proper localization forcing*/
class ContextWrapper(base: Context?) : android.content.ContextWrapper(base) {
        companion object {
            fun wrap(context: Context, newLocale: Locale?): ContextWrapper {
                var context: Context = context
                val res: Resources = context.resources
                val configuration: Configuration = res.configuration
                context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    configuration.setLocale(newLocale)
                    val localeList = LocaleList(newLocale)
                    LocaleList.setDefault(localeList)
                    configuration.setLocales(localeList)
                    context.createConfigurationContext(configuration)
                } else {
                    configuration.setLocale(newLocale)
                    context.createConfigurationContext(configuration)
                }
                return ContextWrapper(context)
            }
        }
    }