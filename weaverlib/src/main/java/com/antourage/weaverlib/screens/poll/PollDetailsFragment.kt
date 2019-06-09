package com.antourage.weaverlib.screens.poll

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.annotation.NonNull
import android.arch.lifecycle.ViewModelProviders
import android.support.annotation.Nullable
import android.support.v7.widget.LinearLayoutManager
import com.antourage.weaverlib.other.models.Poll
import android.widget.TextView
import android.widget.FrameLayout
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.models.AnswersCombined
import com.antourage.weaverlib.screens.base.BaseFragment
import com.antourage.weaverlib.screens.poll.rv.PollAnswersAdapter


class PollDetailsFragment : BaseFragment<PollDetailsViewModel>(), PollAnswersAdapter.AnswerClickedCallback {

    companion object {
        val ARGS_POLL_ID = "args_poll_id"
        val ARGS_STREAM_ID = "args_stream_id"

        fun newInstance(streamId:Int,pollId:String): PollDetailsFragment {
             val bundle = Bundle()
            bundle.putString(ARGS_POLL_ID, pollId)
            bundle.putInt(ARGS_STREAM_ID,streamId)
            val fragment = PollDetailsFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    private var rvPollAnswers: RecyclerView? = null
    private var tvPollTitle: TextView? = null
    private var ivDismissPoll: ImageView? = null
    private var flAnswersFooter: FrameLayout? = null
    private var tvTotalAnswers: TextView? = null

    private val pollObserver:Observer<Poll> = Observer{ poll ->
        if (poll != null) {
            tvPollTitle!!.text = poll.question
        }
    }
    private val answersObserver:Observer<List<AnswersCombined>> = Observer { answers->
        if(answers != null){
            rvPollAnswers!!.layoutManager = LinearLayoutManager(context)
            rvPollAnswers!!.adapter = PollAnswersAdapter(answers, viewModel.isAnswered, this)
            tvTotalAnswers!!.text = viewModel.calculateAllAnswers().toString()
        }

    }


    override fun getLayoutId(): Int {
        return R.layout.fragment_poll_details
    }

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(PollDetailsViewModel::class.java)
    }

    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToObservers()
    }

    private fun subscribeToObservers() {
        viewModel.getPollLiveData().observe(this.viewLifecycleOwner, pollObserver)
        viewModel.getAnswersLiveData().observe(this.viewLifecycleOwner,answersObserver)
    }

    override fun initUi(view: View?) {
        arguments?.let {arguments->
            viewModel.initPollDetails(arguments.getInt(ARGS_STREAM_ID,-1),
                arguments.getString(ARGS_POLL_ID,""))
        }
        view?.let{
            rvPollAnswers = view.findViewById(R.id.rvAnswers)
            tvPollTitle = view.findViewById(R.id.pollTitle)
            ivDismissPoll = view.findViewById(R.id.ivDismissPoll)
            flAnswersFooter = view.findViewById(R.id.flAnswersFooter)
            tvTotalAnswers = view.findViewById(R.id.tvTotalAnswers)
        }
        ivDismissPoll!!.setOnClickListener {
            if (parentFragment != null)
                parentFragment!!.childFragmentManager.popBackStack()
        }
    }


    override fun onAnswerChosen(position: Int) {
        viewModel.onAnswerChosen(position)
    }

}
