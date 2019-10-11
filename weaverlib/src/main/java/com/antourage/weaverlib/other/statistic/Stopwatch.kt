package com.antourage.weaverlib.other.statistic

import android.os.Handler
import java.text.SimpleDateFormat
import java.util.*

class Stopwatch {
    fun onTick(callback: () -> Unit) {
        Handler().postDelayed({ callback.invoke() }, 1000)
    }

    private var startTime: Long = 0
    private var stopTime: Long = 0
    private var running = false
    private var currentDuration: Long = 0

    fun start() {
        this.currentDuration = 0
        this.startTime = System.currentTimeMillis()
        this.running = true
    }

    fun resume() {
        this.startTime = System.currentTimeMillis()
        this.running = true
    }

    fun stop() {
        this.stopTime = System.currentTimeMillis()
        this.running = false
        this.currentDuration += getElapsedTime()
    }


    private fun getElapsedTime(): Long {
        return if (running) {
            System.currentTimeMillis() - startTime
        } else stopTime - startTime
    }

    override fun toString(): String {
        val outputFmt = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
        outputFmt.timeZone = TimeZone.getTimeZone("UTC")

        val t = currentDuration
        return outputFmt.format(t)
    }
}