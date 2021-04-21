package com.antourage.weaverlib

import java.util.*

internal const val TAG = "Log_Global"

class Global {
    companion object {
        var networkAvailable: Boolean = false
        var chosenLocale: Locale? = null
        var defaultLocale: Locale? = null
    }
}