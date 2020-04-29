package com.antourage.weaverlib.other

import java.util.concurrent.TimeUnit

object Constants {
    val secInYear = TimeUnit.DAYS.toSeconds(365)
    val secInMonth = TimeUnit.DAYS.toSeconds(30)
    val secInWeek = TimeUnit.DAYS.toSeconds(7)
    val secInDay = TimeUnit.DAYS.toSeconds(1)
    val secInHour = TimeUnit.HOURS.toSeconds(1)
    val secInMin = TimeUnit.MINUTES.toSeconds(1)
}