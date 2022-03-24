package com.antourage.weaverlib.other

import android.view.View


internal fun View.hideBadge(runnable: Runnable) {
    this.animate()
        .scaleY(0.0f)
        .scaleX(0.0f)
        .alpha(0.0f)
        .setDuration(300)
        .start()
    this.post(runnable)
}

internal fun View.showBadge(runnable: Runnable) {
    this.scaleX = 0.5f
    this.scaleY = 0.5f
    this.alpha = 0.0f
    this.animate()
        .scaleY(1.0f)
        .scaleX(1.0f)
        .alpha(1.0f)
        .setDuration(300)
        .start()
    this.post(runnable)
}
