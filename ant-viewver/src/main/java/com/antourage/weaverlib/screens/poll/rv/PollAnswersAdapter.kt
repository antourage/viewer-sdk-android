package com.antourage.weaverlib.screens.poll.rv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.models.AnswersCombined
import kotlinx.android.synthetic.main.item_poll_unanswered.view.*
import kotlin.math.roundToInt

internal class PollAnswersAdapter(
    private var listOfAnswers: ArrayList<AnswersCombined>,
    private var isAnswered: Boolean,
    private val callback: AnswerClickedCallback) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_UNANSWERED_POLL = 0
        private const val VIEW_ANSWERED_POLL = 1
    }

    //the marker to show animation transition in elements.
    // Should be used only if user has just voted.
    private var shouldShowAnimation: Boolean = false

    interface AnswerClickedCallback {
        fun onAnswerChosen(position: Int)
    }

    fun setNewList(newAnswers: ArrayList<AnswersCombined>, isAnswered: Boolean) {
        if (newAnswers != listOfAnswers || this.isAnswered != isAnswered){
            listOfAnswers.clear()
            listOfAnswers.addAll(newAnswers)
            shouldShowAnimation = !this.isAnswered && isAnswered
            this.isAnswered = isAnswered
            notifyDataSetChanged()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isAnswered) { VIEW_ANSWERED_POLL } else VIEW_UNANSWERED_POLL
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return UnansweredViewHolder(LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_poll_unanswered, viewGroup, false))
        /*return if (viewType == VIEW_ANSWERED_POLL) {
            AnsweredViewHolder(LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_poll_answered, viewGroup, false))
        } else {
            UnansweredViewHolder(LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_poll_unanswered, viewGroup, false))
        }*/
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, pos: Int) {
        (viewHolder as UnansweredViewHolder).bind(listOfAnswers[pos], pos)
        /*if (viewHolder is UnansweredViewHolder) {
            viewHolder.bind(listOfAnswers[pos], pos)
        } else if (viewHolder is AnsweredViewHolder) {
            viewHolder.bind(listOfAnswers[pos], pos)
        }*/
    }

    override fun getItemCount(): Int = listOfAnswers.size

    private fun getPercentage(pos: Int): Double {
        return listOfAnswers[pos].numberAnswered / calculateAllAnswers()
    }

    private fun calculateAllAnswers(): Double {
        var sum = 0
        for (i in listOfAnswers.indices) {
            sum += listOfAnswers[i].numberAnswered
        }
        return sum.toDouble()
    }

    inner class UnansweredViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(result: AnswersCombined, pos: Int) {
            itemView.apply {
                item_poll_bg.isActivated = if (!isAnswered) true else result.isAnsweredByUser
                item_poll_percentage.isActivated = result.isAnsweredByUser
                item_poll_fill_color.isActivated = result.isAnsweredByUser
                item_poll_answer.text = result.answerText
                item_poll_percentage.text = (
                        if (isAnswered)(getPercentage(pos) * 100).roundToInt().toString() + "%" else "")
                item_poll_guideline.setGuidelinePercent(
                    if (isAnswered) getPercentage(pos).toFloat() else 0f)
                item_poll_bg.setOnClickListener { if(!isAnswered) callback.onAnswerChosen(pos) }
                if (isAnswered){
                    item_poll_motion.getConstraintSet(R.id.start_poll_frag)?.let { set ->
                        set.setGuidelinePercent(R.id.item_poll_guideline, 0f)
                    }

                    item_poll_motion.getConstraintSet(R.id.end_poll_frag)?.let { set ->
                        set.setGuidelinePercent(
                            R.id.item_poll_guideline, if (isAnswered) getPercentage(pos).toFloat() else 0f)
                    }
                    if (shouldShowAnimation){
                        stopAnimationItemsIfRequired(pos)
                        item_poll_motion.transitionToEnd()
                    } else {
                        //todo: test this
                        item_poll_motion.progress = 1.0f
                        //item_poll_motion.setTransitionDuration(0)
                    }

                }
            }
        }

        private fun stopAnimationItemsIfRequired(pos: Int){
            if (pos == itemCount - 1) shouldShowAnimation = false
        }
    }

    inner class AnsweredViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(result: AnswersCombined, pos: Int) {
            itemView.apply {
                /*tvAnswer.text = result.answerText
                tvAnswerPercentage.text = ((getPercentage(pos) * 100).roundToInt().toString() + "%")
                trueWidth {
                    val maxWidth = it - dp2px(context, 40f)
                    val params = tvPollLength.layoutParams
                    params.width = (maxWidth * getPercentage(pos)).toInt()
                    if (params.width == 0) {
                        params.width = 10
                    }
                    tvPollLength.layoutParams = params
                }*/
            }
        }
    }
}
