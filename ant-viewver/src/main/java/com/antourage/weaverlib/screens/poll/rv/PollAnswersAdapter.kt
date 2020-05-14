package com.antourage.weaverlib.screens.poll.rv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.models.AnswersCombined
import kotlinx.android.synthetic.main.item_poll.view.*
import kotlin.math.roundToInt

internal class PollAnswersAdapter(
    private var listOfAnswers: ArrayList<AnswersCombined>,
    private var isAnswered: Boolean,
    private val callback: AnswerClickedCallback
) : RecyclerView.Adapter<PollAnswersAdapter.PollViewHolder>() {

    //the marker to show animation transition in elements.
    // Should be used only if user has just voted.
    private var shouldShowAnimation: Boolean = false

    interface AnswerClickedCallback {
        fun onAnswerChosen(position: Int)
    }

    fun setNewList(newAnswers: ArrayList<AnswersCombined>, isAnswered: Boolean) {
        if (newAnswers != listOfAnswers || this.isAnswered != isAnswered){
            setAnimationIfRequired(isAnswered)
            listOfAnswers.clear()
            listOfAnswers.addAll(newAnswers)
            this.isAnswered = isAnswered
            notifyDataSetChanged()
        }
    }

    /**
     * Checks whether animation should be applicable for itemViews;
     * Animation should be shown only when user has just voted;
     * So, the check on empty list means that user wasn't voting as it's just adapter list creation;
     */
    private fun setAnimationIfRequired(newIisAnswered: Boolean) {
        shouldShowAnimation  = if(listOfAnswers.isEmpty()){
            false
        } else {
            !this.isAnswered && newIisAnswered
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): PollViewHolder {
        return PollViewHolder(LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_poll, viewGroup, false))
    }

    override fun onBindViewHolder(viewHolder: PollViewHolder, pos: Int) {
        viewHolder.bind(listOfAnswers[pos], pos)
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

    inner class PollViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
                if(!isAnswered) {
                    item_poll_bg.setOnClickListener { if(!isAnswered) callback.onAnswerChosen(pos) }
                } else {
                    item_poll_bg.isClickable = false
                }
                if (isAnswered){
                    item_poll_motion.getConstraintSet(R.id.start_poll_frag)
                        ?.setGuidelinePercent(R.id.item_poll_guideline, 0f)

                    item_poll_motion.getConstraintSet(R.id.end_poll_frag)?.setGuidelinePercent(
                        R.id.item_poll_guideline, if (isAnswered) getPercentage(pos).toFloat() else 0f)
                    if (shouldShowAnimation){
                        stopAnimatingItemsIfRequired(pos)
                        item_poll_motion.transitionToEnd()
                    } else {
                        //directly sets motionLayout to end set without animation
                        item_poll_motion.progress = 1.0f
                    }
                }
            }
        }

        /**
         * If the item is the last one in list, so the animation for voting was already shown and
         * shouldn't be shown again in this adapter;
         */
        private fun stopAnimatingItemsIfRequired(pos: Int){
            if (pos == itemCount - 1) {
                shouldShowAnimation = false
            }
        }
    }
}
