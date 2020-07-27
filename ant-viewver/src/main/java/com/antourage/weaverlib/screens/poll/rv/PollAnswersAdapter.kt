package com.antourage.weaverlib.screens.poll.rv

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.models.AdBanner
import com.antourage.weaverlib.other.models.AnswersCombined
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.item_poll.view.*
import kotlinx.android.synthetic.main.item_poll_banner.view.*
import kotlin.math.roundToInt

internal class PollAnswersAdapter(
    private var listOfAnswers: ArrayList<AnswersCombined>,
    private var isAnswered: Boolean,
    private var banner: AdBanner? = null,
    private val answerClick: AnswerClickedCallback,
    private val bannerClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    //the marker to show animation transition in elements.
    // Should be used only if user has just voted.
    private var shouldShowAnimation: Boolean = false

    companion object {
        const val TYPE_ITEM = 0
        const val TYPE_BANNER = 1
    }

    interface AnswerClickedCallback {
        fun onAnswerChosen(position: Int)
    }

    fun setNewList(newAnswers: ArrayList<AnswersCombined>, isAnswered: Boolean) {
        if (newAnswers != listOfAnswers || this.isAnswered != isAnswered) {
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
        shouldShowAnimation = if (listOfAnswers.isEmpty()) {
            false
        } else {
            /*!this.isAnswered && newIisAnswered*/
            newIisAnswered
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == TYPE_ITEM) {
            val inflater = LayoutInflater.from(parent.context)
            val itemView: View = inflater.inflate(R.layout.item_poll, parent, false)
            val answerHeightSpace = parent.height - getBannerHeightWithPadding(parent.width, parent.context)

            var itemHeight =  (answerHeightSpace * 0.25).toInt() -
                    parent.context.resources.getDimension(R.dimen.poll_divider_size).toInt()
            val maxAnswerHeight = parent.context.resources.getDimension(R.dimen.poll_answer_max_size).toInt()
            if (itemHeight >= maxAnswerHeight){
                itemHeight = maxAnswerHeight
            }
            val layoutParams = itemView.layoutParams
            layoutParams.height = itemHeight
            itemView.layoutParams = layoutParams

            return PollViewHolder(itemView, itemHeight)
        } else {
            val itemView= LayoutInflater.from(parent.context).inflate(R.layout.item_poll_banner, parent, false)
            val bannerWidth =  parent.width - ((parent.context.resources.getDimension(R.dimen.poll_banner_padding)) * 2)
            return BannerHolder(itemView, bannerWidth.toInt())
        }
    }

    private fun getBannerHeightWithPadding(layoutWidth: Int, context : Context): Float{
        val bannerWidth =  layoutWidth - ((context.resources.getDimension(R.dimen.poll_banner_padding)) * 2)
        return ((bannerWidth/320)*50) + (context.resources.getDimension(R.dimen.poll_divider_size) * 2.5f)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
        if (holder is PollViewHolder) {
            holder.bind(listOfAnswers[pos], pos)
        } else {
            (holder as BannerHolder).bind()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == itemCount - 1)  TYPE_BANNER else TYPE_ITEM
    }

    override fun getItemCount(): Int = listOfAnswers.size + 1

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

    inner class PollViewHolder(itemView: View, private var itemHeight: Int) : RecyclerView.ViewHolder(itemView) {
        fun bind(result: AnswersCombined, pos: Int) {
            itemView.apply {
                item_poll_card.radius = itemHeight.toFloat() /2

                val prevPercentage =
                    (item_poll_guideline.layoutParams as ConstraintLayout.LayoutParams).guidePercent
                val newPercentage = getPercentage(pos).toFloat()
                item_poll_bg.isActivated = if (!isAnswered) true else result.isAnsweredByUser
                item_poll_percentage.isActivated = result.isAnsweredByUser
                item_poll_fill_color.isActivated = result.isAnsweredByUser
                item_poll_answer.text = result.answerText
                item_poll_percentage.text = (
                        if (isAnswered) (getPercentage(pos) * 100).roundToInt()
                            .toString() + "%" else "")
                if (!isAnswered) {
                    item_poll_bg.setOnClickListener { if (!isAnswered) answerClick.onAnswerChosen(pos) }
                } else {
                    item_poll_bg.isClickable = false
                }

                if (isAnswered) {
                    if (shouldShowAnimation) {
                        if (item_poll_motion.progress == 1.0f) {
                            item_poll_motion.setConstraintFromAnsweredToNewAnswer(
                                prevPercentage,
                                newPercentage
                            )
                            item_poll_motion.animateWidget()
                        } else {
                            item_poll_motion.setConstraintToJustAnswered(newPercentage)
                            item_poll_motion.animateWidget()
                        }
                        stopAnimatingItemsIfRequired(pos)
                    } else {
                        item_poll_motion.setAnsweredStateWithoutAnimation(newPercentage)
                    }
                }
            }
        }

        /**
         * If the item is the last one in list, so the animation for voting was already shown and
         * shouldn't be shown again in this adapter;
         */
        private fun stopAnimatingItemsIfRequired(pos: Int) {
            if (pos == itemCount - 1) {
                shouldShowAnimation = false
            }
        }
    }

    inner class BannerHolder(view: View, private var itemHeight: Int) : RecyclerView.ViewHolder(view) {

        fun bind() {
            itemView.apply {
                if (banner?.imageUrl != null) {
                    cardPollBanner.visibility = View.VISIBLE
                    ivPollBanner.setOnClickListener {
                        banner?.externalUrl?.let {
                            if (it.startsWith("https://") || it.startsWith("http://")){
                                bannerClick(it)
                            }
                        }
                    }
                    Picasso.get().load(banner!!.imageUrl).resize(0, itemHeight)
                        .into(ivPollBanner)
                } else{
                    cardPollBanner.visibility = View.INVISIBLE
                }
            }
        }
    }
}
