package com.antourage.weaverlib.screens.poll.rv

import android.content.Context
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.dp2px
import com.antourage.weaverlib.other.models.AnswersCombined
import com.antourage.weaverlib.other.trueWidth

internal class PollAnswersAdapter(
    private val listOfAnswers: List<AnswersCombined>,
    private val isAnswered: Boolean,
    private val callback: AnswerClickedCallback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var context: Context? = null

    private val listOfBackgrounds = object : ArrayList<Int>() {
        init {
            add(R.drawable.antourage_rounded_orange_bg)
            add(R.drawable.antourage_rounded_bleak_pink_bg)
            add(R.drawable.antourage_rounded_blue_bg)
            add(R.drawable.antourage_rounded_green_bg)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = viewGroup.context
        return if (viewType == VIEW_ANSWERED_POLL) {
            AnsweredViewHolder(
                LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.item_answered_poll, viewGroup, false)
            )
        } else {
            UnansweredViewHolder(
                LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.item_unanswered_poll, viewGroup, false)
            )
        }
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, i: Int) {
        if (viewHolder is UnansweredViewHolder) {
            viewHolder.btnAnswer.background = ResourcesCompat.getDrawable(
                context!!.resources,
                listOfBackgrounds[i], null
            )
            viewHolder.btnAnswer.text = listOfAnswers[i].answerText
            viewHolder.btnAnswer.setOnClickListener { callback.onAnswerChosen(viewHolder.getAdapterPosition()) }
        } else {
            (viewHolder as AnsweredViewHolder).tvAnswer.text = listOfAnswers[i].answerText
            viewHolder.tvAnswerPercentage.text = (Math.round(getPercentage(i) * 100).toString()
                    + "%")
            viewHolder.tvPollLength.background = ResourcesCompat.getDrawable(
                context!!.resources,
                listOfBackgrounds[i], null
            )
            viewHolder.itemView.trueWidth {
                val maxWidth = it - dp2px(context!!, 40f)
                val params = viewHolder.tvPollLength.layoutParams
                params.width = (maxWidth * getPercentage(i)).toInt()
                if (params.width == 0) {
                    params.width = 10
                }
                viewHolder.tvPollLength.layoutParams = params
            }
        }
    }

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

    override fun getItemCount(): Int {
        return listOfAnswers.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (isAnswered) {
            VIEW_ANSWERED_POLL
        } else
            VIEW_UNANSWERED_POLL
    }

    internal inner class UnansweredViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var btnAnswer: Button = itemView.findViewById(R.id.btnAnswer)

    }

    internal inner class AnsweredViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var tvAnswer: TextView = itemView.findViewById(R.id.tvAnswer)
        var tvPollLength: TextView = itemView.findViewById(R.id.tvPollLength)
        var tvAnswerPercentage: TextView = itemView.findViewById(R.id.tvAnswerPercentage)

    }

    interface AnswerClickedCallback {
        fun onAnswerChosen(position: Int)
    }

    companion object {
        private const val VIEW_UNANSWERED_POLL = 0
        private const val VIEW_ANSWERED_POLL = 1
    }
}
