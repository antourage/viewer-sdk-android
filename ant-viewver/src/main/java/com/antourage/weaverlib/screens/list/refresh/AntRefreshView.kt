package com.antourage.weaverlib.screens.list.refresh

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.antourage.weaverlib.Global
import com.antourage.weaverlib.R

class AntRefreshView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AntBaseProgressBar(context, attrs, defStyleAttr) {

    private val animatedDrawable = context.let {
        AnimatedVectorDrawableCompat.create(
            it,
            R.drawable.antourage_pull_refresh_background_animation
        )
    }

    private var stripes = ImageView(context)
    private var animatedBackground = ImageView(context)

    init {
        stripes.setImageResource(R.drawable.pull1)
    }

    override fun setParent(parent: AntPullToRefreshView) {
        mParent = parent
        setUpView()
    }

    override fun setPercent(percent: Float) {
        mPercent = percent

        when (percent.toInt()) {
            in 0..3 -> {
                stripes.setImageResource(R.drawable.pull1)
            }
            in 4..7 -> {
                stripes.setImageResource(R.drawable.pull2)
            }
            in 8..11 -> {
                stripes.setImageResource(R.drawable.pull3)
            }
            in 12..15 -> {
                stripes.setImageResource(R.drawable.pull4)
            }
            in 16..19 -> {
                stripes.setImageResource(R.drawable.pull5)
            }
            in 20..23 -> {
                stripes.setImageResource(R.drawable.pull6)
            }
            in 24..27 -> {
                stripes.setImageResource(R.drawable.pull7)
            }
            in 28..31 -> {
                stripes.setImageResource(R.drawable.pull8)
            }
            in 32..35 -> {
                stripes.setImageResource(R.drawable.pull9)
            }
            in 36..39 -> {
                stripes.setImageResource(R.drawable.pull10)
            }
            in 40..43 -> {
                stripes.setImageResource(R.drawable.pull11)
            }
            in 44..47 -> {
                stripes.setImageResource(R.drawable.pull12)
            }
            in 48..51 -> {
                stripes.setImageResource(R.drawable.pull13)
            }
            in 52..55 -> {
                stripes.setImageResource(R.drawable.pull14)
            }
            in 56..59 -> {
                stripes.setImageResource(R.drawable.pull15)
            }
            in 60..63 -> {
                stripes.setImageResource(R.drawable.pull16)
            }
            in 64..67 -> {
                stripes.setImageResource(R.drawable.pull17)
            }
            in 68..71 -> {
                stripes.setImageResource(R.drawable.pull18)
            }
            in 72..75 -> {
                stripes.setImageResource(R.drawable.pull19)
            }
            in 76..79 -> {
                stripes.setImageResource(R.drawable.pull20)
            }
            in 80..83 -> {
                stripes.setImageResource(R.drawable.pull21)
            }
            in 84..87 -> {
                stripes.setImageResource(R.drawable.pull22)
            }
            in 88..91 -> {
                stripes.setImageResource(R.drawable.pull23)
            }
            in 92..95 -> {
                stripes.setImageResource(R.drawable.pull24)
            }
            in 96..100 -> {
                stripes.setImageResource(R.drawable.pull25)
            }
            else -> {
                stripes.setImageResource(R.drawable.pull25)
            }
        }
    }

    private fun setUpView() {
        mParent.setCustomView(stripes, dp2px(100), dp2px(40))
        mParent.setCustomAnimationView(animatedBackground)
    }

    override fun start() {
        if(Global.networkAvailable){
            animatedBackground.setImageDrawable(animatedDrawable)
            animatedDrawable?.start()
            animatedDrawable?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback(){
                override fun onAnimationEnd(drawable: Drawable?) {
                    super.onAnimationEnd(drawable)
                    animatedDrawable.start()
                }
            })
        }
        stripes.animate().translationYBy(-300f).setDuration(300).start()

    }

    override fun stop() {
        animatedDrawable?.stop()
        animatedDrawable?.clearAnimationCallbacks()
        stripes.animate().translationY(0f).setDuration(300).start()
    }
}