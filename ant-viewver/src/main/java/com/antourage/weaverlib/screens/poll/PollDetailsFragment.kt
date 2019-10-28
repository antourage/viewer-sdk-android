package com.antourage.weaverlib.screens.poll

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.content.res.Configuration
import android.os.Bundle
import androidx.annotation.Nullable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.di.injector
import com.antourage.weaverlib.other.dp2px
import com.antourage.weaverlib.other.models.AnswersCombined
import com.antourage.weaverlib.other.models.Poll
import com.antourage.weaverlib.other.reObserve
import com.antourage.weaverlib.other.ui.MarginItemDecoration
import com.antourage.weaverlib.screens.base.BaseFragment
import com.antourage.weaverlib.screens.poll.rv.PollAnswersAdapter

internal class PollDetailsFragment : BaseFragment<PollDetailsViewModel>(),
    PollAnswersAdapter.AnswerClickedCallback {

    companion object {
        val ARGS_POLL_ID = "args_poll_id"
        val ARGS_STREAM_ID = "args_stream_id"

        fun newInstance(streamId: Int, pollId: String): PollDetailsFragment {
            val bundle = Bundle()
            bundle.putString(ARGS_POLL_ID, pollId)
            bundle.putInt(ARGS_STREAM_ID, streamId)
            val fragment = PollDetailsFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    private var rvPollAnswers: RecyclerView? = null
    private var tvPollTitle: TextView? = null
    private var ivDismissPoll: ImageView? = null
    private var tvTotalAnswers: TextView? = null

    private val pollObserver: Observer<Poll> = Observer { poll ->
        if (poll != null) {
            tvPollTitle!!.text = poll.question
        }
    }
    private val answersObserver: Observer<List<AnswersCombined>> = Observer { answers ->
        if (answers != null) {
            rvPollAnswers?.apply {
                layoutManager = LinearLayoutManager(context)
                adapter =
                    PollAnswersAdapter(answers, viewModel.isAnswered, this@PollDetailsFragment)
            }
            tvTotalAnswers?.text = viewModel.calculateAllAnswers().toString()
        }
    }

    override fun getLayoutId() = R.layout.fragment_poll_details

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, activity?.injector?.getPollViewModelFactory())
            .get(PollDetailsViewModel::class.java)
    }

    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToObservers()
    }

    private fun subscribeToObservers() {
        viewModel.getPollLiveData().observe(this.viewLifecycleOwner, pollObserver)
        viewModel.getAnswersLiveData().observe(this.viewLifecycleOwner, answersObserver)
    }

    override fun initUi(view: View?) {
        arguments?.let { arguments ->
            viewModel.initPollDetails(
                arguments.getInt(ARGS_STREAM_ID, -1),
                arguments.getString(ARGS_POLL_ID, "")
            )
        }
        view?.let {
            rvPollAnswers = view.findViewById(R.id.rvAnswers)
            context?.let { context ->
                rvPollAnswers?.addItemDecoration(
                    MarginItemDecoration(
                        dp2px(context, 20f).toInt()
                    )
                )
            }
            tvPollTitle = view.findViewById(R.id.pollTitle)
            ivDismissPoll = view.findViewById(R.id.ivDismissPoll)
            tvTotalAnswers = view.findViewById(R.id.tvTotalAnswers)
        }
        ivDismissPoll?.setOnClickListener {
            if (parentFragment != null)
                parentFragment?.childFragmentManager?.popBackStack()
        }
    }

    override fun onAnswerChosen(position: Int) {
        viewModel.onAnswerChosen(position)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        viewModel.getPollLiveData().reObserve(this.viewLifecycleOwner, pollObserver)
        viewModel.getAnswersLiveData().reObserve(this.viewLifecycleOwner, answersObserver)
    }

}