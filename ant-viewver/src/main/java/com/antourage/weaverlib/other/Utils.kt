package com.antourage.weaverlib.other

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.models.PortalConfig
import com.antourage.weaverlib.other.models.WebConfig

internal fun generateConfig(context: Context, config: WebConfig): PortalConfig {
    val colorWidgetBorder: Int = try {
        Color.parseColor(config.colorWidgetBorder)
    } catch (e: Exception) {
        ContextCompat.getColor(context, R.color.ant_colorWidgetBorder)
    }

    val colorCtaBg: Int = try {
        Color.parseColor(config.colorCtaBg)
    } catch (e: Exception) {
        ContextCompat.getColor(context, R.color.ant_ctaBackground)
    }

    val colorCtaText: Int = try {
        Color.parseColor(config.colorCtaText)
    } catch (e: Exception) {
        ContextCompat.getColor(context, R.color.ant_white)
    }

    val colorLive: Int = try {
        Color.parseColor(config.colorLive)
    } catch (e: Exception) {
        ContextCompat.getColor(context, R.color.ant_colorLive)
    }

    val colorTitleBg: Int = try {
        Color.parseColor(config.colorTitleBg)
    } catch (e: Exception) {
        ContextCompat.getColor(context, R.color.ant_white)
    }

    val colorNameBg: Int = try {
        Color.parseColor(config.colorNameBg)
    } catch (e: Exception) {
        ContextCompat.getColor(context, R.color.ant_white)
    }

    val colorNameText: Int = try {
        Color.parseColor(config.colorNameText)
    } catch (e: Exception) {
        ContextCompat.getColor(context, R.color.ant_black)
    }

    val colorTitleText: Int = try {
        Color.parseColor(config.colorTitleText)
    } catch (e: Exception) {
        ContextCompat.getColor(context, R.color.ant_black)
    }

    return PortalConfig(
        colorWidgetBorder,
        colorCtaBg,
        colorCtaText,
        colorLive,
        colorTitleBg,
        colorNameBg,
        colorNameText,
        colorTitleText
    )
}