package com.antourage.weaverlib.ui.fab

import android.animation.ObjectAnimator
import android.animation.RectEvaluator
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.constraintlayout.widget.ConstraintLayout
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.models.PortalState
import kotlinx.android.synthetic.main.antourage_labels_layout.view.*
import java.util.regex.Matcher
import java.util.regex.Pattern


class WidgetLabelsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    var isLabelShown = false
    private val anim: Animation = AlphaAnimation(0.0f, 1.0f)

    init {
        init()
    }

    private fun setTitle(text: String) {
        val maxLength = 25
        if (text.length > maxLength) {
            tvLiveSecondLine.visibility = View.VISIBLE
            val p: Pattern = Pattern.compile("\\G\\s*(.{1,$maxLength})(?=\\s|$)", Pattern.DOTALL)
            val m: Matcher = p.matcher(text)
            val lines = mutableListOf<String>()
            while (m.find()) lines.add(m.group(1))
            tvLiveFirstLine.text = lines[0]
            tvLiveSecondLine.text = lines[1]
        } else {
            tvLiveFirstLine.text = text
            tvLiveSecondLine.visibility = View.GONE
            return
        }
    }

    private fun formatCtaText(text: String): String {
        val maxLenght = 25
        if (text.length > maxLenght) {
            val p: Pattern = Pattern.compile("\\G\\s*(.{1,$maxLenght})(?=\\s|$)", Pattern.DOTALL)
            val m: Matcher = p.matcher(text)
            val lines = mutableListOf<String>()
            while (m.find()) lines.add(m.group(1))
            try {
                return "${lines[0]}\n${lines[1]}"
            }catch (e: Exception){
                return text
            }
        } else {
            return text
        }
    }

    private fun init() {
        View.inflate(context, R.layout.antourage_labels_layout, this)
        anim.duration = 500
        anim.repeatMode = Animation.REVERSE
        anim.repeatCount = Animation.INFINITE
    }

    fun revealView(data: PortalState?) {
        data?.apply {
            title?.let { setTitle(it) }
            ctaLabel?.let { btnCta.text = formatCtaText(it) }
            live?.let {
                if (it) {
                    dotView?.visibility = View.VISIBLE
                    dotView.startAnimation(anim)
                    tvStreamerName?.visibility = View.VISIBLE
                    name?.let { tvStreamerName.text = it }
                } else {
                    tvStreamerName?.visibility = View.INVISIBLE
                    dotView?.visibility = View.GONE
                }
            }
        }

        liveLabelsContainer.post {
            isLabelShown = true
            val local = Rect()
            liveLabelsContainer.getLocalVisibleRect(local)
            val from = Rect(local)
            val to = Rect(local)
            from.left = from.right

            val anim: ObjectAnimator = ObjectAnimator.ofObject(
                liveLabelsContainer,
                "clipBounds",
                RectEvaluator(),
                from, to
            )

            anim.duration = 1000
            anim.start()
            liveLabelsContainer.visibility = View.VISIBLE
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        super.onTouchEvent(event)
        return isLabelShown
    }

    fun hideView(shouldAnimate: Boolean = true) {
        if (!isLabelShown) return
        if (shouldAnimate) {
            val local = Rect()
            liveLabelsContainer.getLocalVisibleRect(local)
            val from = Rect(local)
            val to = Rect(local)

            to.left = to.right

            val anim: ObjectAnimator = ObjectAnimator.ofObject(
                liveLabelsContainer,
                "clipBounds",
                RectEvaluator(),
                from, to
            )

            anim.duration = 1000
            anim.start()
        } else {
            liveLabelsContainer.visibility = View.INVISIBLE
        }
        dotView.clearAnimation()
        isLabelShown = false
    }
}