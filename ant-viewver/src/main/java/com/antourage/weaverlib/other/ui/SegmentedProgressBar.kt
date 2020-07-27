package com.antourage.weaverlib.other.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.models.CurtainRangeMillis
import kotlin.math.min

internal class SegmentedProgressBar : View {

    internal data class CurtainRangeSegments(val start: Float, val end: Float)

    private var barHeight = 0f
    private var progressColor = 0
    private var segmentColor = 0
    private var viewBackgroundColor = 0

    private var progressPaint: Paint? = null
    private val min = 0
    private var max = 0
    private var progress = 0
    private var progressSegment = 0f
    private var curtainsSegments = ArrayList<CurtainRangeSegments>()
    private var curtainsRange = ArrayList<CurtainRangeMillis>()

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int)
            : super(context, attrs, defStyle) {
        init(attrs)
    }

    internal fun setMax(max: Int) {
        if (this.max != max){
            this.max = max
            //todo: uncommit if required
            /*if (curtainsSegments.isNotEmpty()){
                curtainsSegments.clear()
            }*/
            postInvalidate()
        }
    }

    private fun init(attrs: AttributeSet) {
        progressPaint = Paint()
        progressPaint?.style = Paint.Style.FILL_AND_STROKE
        val typedArray =
            context.theme.obtainStyledAttributes(attrs, R.styleable.SegmentedProgressBar, 0, 0)
        try {
            setBarHeight(
                typedArray.getDimensionPixelSize(
                    R.styleable.SegmentedProgressBar_barHeight,
                    10
                ).toFloat()
            )

            setProgressColor(
                typedArray.getColor(
                    R.styleable.SegmentedProgressBar_progressColor,
                    Color.BLACK
                )
            )

            setViewBackgroundColor(
                typedArray.getColor(
                    R.styleable.SegmentedProgressBar_backgroundColor,
                    Color.RED
                )
            )

            setSegmentColor(
                typedArray.getColor(
                    R.styleable.SegmentedProgressBar_segmentColor,
                    Color.YELLOW
                )
            )
        } finally {
            typedArray.recycle()
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()

        // save our added state - progress and goal
        bundle.putInt("progress", progress)
        bundle.putInt("max", max)
        bundle.putParcelableArrayList("curtains", curtainsRange)

        // save super state
        bundle.putParcelable("superState", super.onSaveInstanceState())
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        var restoredState = state
        if (state is Bundle) {
            // restore our added state - progress, max and curtain
            setProgress(state.getInt("progress"))
            setMax(state.getInt("max"))
            setListOfCurtains(
                state.getParcelableArrayList<CurtainRangeMillis>("curtains")
                    as ArrayList<CurtainRangeMillis>)

            // restore super state
            restoredState = state.getParcelable("superState")
        }
        super.onRestoreInstanceState(restoredState)
    }

    override fun onDraw(canvas: Canvas) {
        val halfHeight = height / 2
        refreshProgressSegment()
        val progressEndX = (width * progressSegment).toInt()
        progressPaint!!.strokeWidth = barHeight

        // draws the background
        progressPaint!!.color = viewBackgroundColor
        canvas.drawLine(
            0f, halfHeight.toFloat(), width.toFloat(),
            halfHeight.toFloat(), progressPaint!!
        )

        // draws the curtain segments
        if (curtainsSegments.isNotEmpty()){
            progressPaint!!.color = segmentColor
            curtainsSegments.forEach { segment ->
                canvas.drawLine(
                    width * segment.start, halfHeight.toFloat(), width * segment.end,
                    halfHeight.toFloat(), progressPaint!!
                )
            }
        }

        // draws the progress
        progressPaint!!.color = progressColor
        canvas.drawLine(
            0f, halfHeight.toFloat(), progressEndX.toFloat(),
            halfHeight.toFloat(), progressPaint!!
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val specHeight = MeasureSpec.getSize(heightMeasureSpec)
        val height: Int
        height = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> specHeight
            MeasureSpec.AT_MOST -> min(barHeight, specHeight.toFloat()).toInt()
            MeasureSpec.UNSPECIFIED -> specHeight
            else -> specHeight
        }

        // must call this, otherwise the app will crash
        setMeasuredDimension(width, height)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    internal fun setProgress(progress: Int, shouldUpdateView : Boolean = true) {
        val currProgress = constrain(progress, min, max)
        if (currProgress == this.progress) return
        this.progress = progress
        if (shouldUpdateView) postInvalidate()
    }

    /**
     * Should be called after max was set;
     */
    private fun setListOfCurtains(curtains: List<CurtainRangeMillis>){
        if (curtainsRange != curtains){
            curtainsRange.clear()
            curtainsSegments.clear()
            if (curtains.isNotEmpty()){
                curtainsRange = ArrayList(curtains.map { it.copy() })
                curtainsRange.forEach {
                    curtainsSegments.add(CurtainRangeSegments(
                        getTimePointSegment(it.start),
                        getTimePointSegment(it.end)
                    ))
                }
            }
            postInvalidate()
        }
    }

    fun setListOfCurtainsAndMax(curtains: List<CurtainRangeMillis>, max: Int){
        setMax(max)
        setListOfCurtains(curtains)
    }

    private fun getTimePointSegment(point : Long) : Float {
        val pointInt  = constrain(point.toInt(), min, max)
        val range = (max - min).toFloat()
        return  (if (range > 0) (pointInt - min) / range else 0f)
    }

    private fun refreshProgressSegment() {
        val range = (max - min).toFloat()
        progressSegment = (if (range > 0) (progress - min) / range else 0f)
    }

    private fun constrain(amount: Int, low: Int, high: Int): Int {
        return if (amount < low) low else if (amount > high) high else amount
    }

    private fun setBarHeight(barHeight: Float) {
        this.barHeight = barHeight
        postInvalidate()
    }

    private fun setProgressColor(progressColor: Int) {
        this.progressColor = progressColor
        postInvalidate()
    }

    private fun setSegmentColor(segmentColor: Int) {
        this.segmentColor = segmentColor
        postInvalidate()
    }

    private fun setViewBackgroundColor(viewBackgroundColor: Int) {
        this.viewBackgroundColor = viewBackgroundColor
        postInvalidate()
    }
}