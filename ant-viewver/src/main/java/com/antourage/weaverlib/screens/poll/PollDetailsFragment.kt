package com.antourage.weaverlib.screens.poll

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.models.AdBanner
import com.antourage.weaverlib.other.models.AnswersCombined
import com.antourage.weaverlib.other.models.Poll
import com.antourage.weaverlib.other.reObserve
import com.antourage.weaverlib.other.ui.TopBottomItemDecoration
import com.antourage.weaverlib.screens.base.BaseFragment
import com.antourage.weaverlib.screens.poll.rv.PollAnswersAdapter
import kotlinx.android.synthetic.main.fragment_poll_details.*

internal class PollDetailsFragment : BaseFragment<PollDetailsViewModel>(),
    PollAnswersAdapter.AnswerClickedCallback {

    companion object {
        const val ARGS_POLL_ID = "args_poll_id"
        const val ARGS_STREAM_ID = "args_stream_id"
        const val ARGS_USER_ID = "args_user_id"
        const val ARGS_BANNER = "args_banner"

        fun newInstance(streamId: Int, pollId: String, userId: String?, banner: AdBanner?): PollDetailsFragment {
            val bundle = Bundle()
            bundle.putString(ARGS_POLL_ID, pollId)
            bundle.putInt(ARGS_STREAM_ID, streamId)
            bundle.putString(ARGS_USER_ID, userId)
            bundle.putParcelable(ARGS_BANNER, banner)
            val fragment = PollDetailsFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    private var tvPollTitle: TextView? = null
    private var ivDismissPoll: ImageView? = null

    private val pollObserver: Observer<Poll> = Observer { poll ->
        if (poll != null) {
            tvPollTitle!!.text = poll.question
            if (!poll.isActive) closePoll()
        }
    }
    private val answersObserver: Observer<List<AnswersCombined>> = Observer { answers ->
        if (answers != null) {
            (rvAnswers.adapter as PollAnswersAdapter).setNewList(answers as ArrayList, viewModel.isAnswered)
        }
    }

    override fun getLayoutId() = R.layout.fragment_poll_details

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(PollDetailsViewModel::class.java)
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
                arguments.getString(ARGS_POLL_ID, ""),
                arguments.getString(ARGS_USER_ID, null),
                arguments.getParcelable(ARGS_BANNER)
            )
        }
        view?.let {
            rvAnswers.apply {
                layoutManager = LinearLayoutManager(context)
                addItemDecoration(TopBottomItemDecoration(context.resources.getDimension(R.dimen.poll_divider_size).toInt()))
                adapter = PollAnswersAdapter(ArrayList(),
                    viewModel.isAnswered,
                    viewModel.getBanner(),
                    this@PollDetailsFragment,
                    bannerClick = {
                         val intent = Intent(Intent.ACTION_VIEW)
                         intent.data = Uri.parse(it)
                         startActivity(intent)
                    })
            }
            tvPollTitle = view.findViewById(R.id.pollTitle)
            ivDismissPoll = view.findViewById(R.id.ivDismissPoll)
        }
        ivDismissPoll?.setOnClickListener {closePoll() }
    }

    private fun closePoll(){
        if (parentFragment != null)
            parentFragment?.childFragmentManager?.popBackStack(
                null,
                POP_BACK_STACK_INCLUSIVE
            )
    }

    override fun onAnswerChosen(position: Int) {
        viewModel.userId?.let { userId ->
            viewModel.onAnswerChosen(position, userId)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        viewModel.getPollLiveData().reObserve(this.viewLifecycleOwner, pollObserver)
        viewModel.getAnswersLiveData().reObserve(this.viewLifecycleOwner, answersObserver)
    }
}
