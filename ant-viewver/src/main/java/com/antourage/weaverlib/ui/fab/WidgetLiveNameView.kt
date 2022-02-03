package com.antourage.weaverlib.ui.fab

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.RectEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Display
import android.view.View
import android.view.animation.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.antourage.weaverlib.R
import kotlinx.android.synthetic.main.antourage_fab_layout.view.*
import kotlinx.android.synthetic.main.antourage_live_name_layout.view.*
import java.util.logging.Handler
import java.util.regex.Matcher
import java.util.regex.Pattern

class WidgetLiveNameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        init()
    }

    fun setText(text: String) {
//        val maxLenght = 25
//        if(text.length > maxLenght){
//            tvLiveSecondLine.visibility = View.VISIBLE
//            val p: Pattern = Pattern.compile("\\G\\s*(.{1,$maxLenght})(?=\\s|$)", Pattern.DOTALL)
//            val m: Matcher = p.matcher(text)
//            val lines = mutableListOf<String>()
//            while (m.find()) lines.add(m.group(1))
//            tvLiveFirstLine.text = lines[0]
//            tvLiveSecondLine.text = lines[1]
//        }else{
//            tvLiveFirstLine.text = text
//            tvLiveSecondLine.visibility = View.GONE
//            return
//        }
    }

    private fun init() {
        View.inflate(context, R.layout.antourage_live_name_layout, this)
        startDotAnimation()
    }

    fun revealView(){
        revealLabel()
    }

    fun hideView(){
        Log.e("WTF", "hideLabels: ????", )
        hideLabels()
    }

    private fun revealLabel() {
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

    private fun hideLabels() {
        Log.e("WTF", "hideLabels: ", )
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
    }

    private fun startDotAnimation() {
        val anim: Animation = AlphaAnimation(0.0f, 1.0f)
        anim.duration = 500 //You can manage the blinking time with this parameter

        anim.repeatMode = Animation.REVERSE
        anim.repeatCount = Animation.INFINITE
        dotView.startAnimation(anim)
    }

//    internal fun hideView(){
//        if(this::fab.isInitialized){
//            circularHideOverlay(this, fab)
//        }else{
//            this.visibility = GONE
//        }
//    }
}