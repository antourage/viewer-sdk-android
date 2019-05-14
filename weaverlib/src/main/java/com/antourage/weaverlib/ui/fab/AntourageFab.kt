package com.antourage.weaverlib.ui.fab

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.MotionScene
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.base.AppExecutors
import com.antourage.weaverlib.other.networking.base.Resource
import com.antourage.weaverlib.other.networking.base.State
import com.antourage.weaverlib.screens.base.AntourageActivity
import com.antourage.weaverlib.screens.base.BaseViewModel
import com.antourage.weaverlib.screens.base.Repository
import kotlinx.android.synthetic.main.antourage_fab_layout.view.*

class AntourageFab @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), FabActionHandler {
    
    val listOfSeenStreams = mutableListOf<Int>()

    var streamResponse: LiveData<Resource<List<StreamResponse>>> = MutableLiveData()

    val transitionListener = object :MotionLayout.TransitionListener{
        override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {

        }

        override fun allowsTransition(p0: MotionScene.Transition?): Boolean {
            return true
        }

        override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {
        }

        override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {
        }

        override fun onTransitionCompleted(p0: MotionLayout?, currentId: Int) {
            if (currentId == R.id.start){
                expandableLayout.visibility = View.INVISIBLE
            }
        }
    }



    val handlerCall = Handler()

    init {
        View.inflate(context,R.layout.antourage_fab_layout,this)
        val intent = Intent(context, AntourageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        floatingActionButton.setOnClickListener {
            context.startActivity(intent)
        }
        fabExpantion.setOnClickListener { context.startActivity(intent) }
        AntourageFabLifecycleObserver.registerActionHandler(this)
    }


    override fun onPause() {
//        expandableLayout.visibility = View.INVISIBLE
//        expandableLayout.setTransitionListener(null)
    }

    override fun onResume() {
        expandableLayout.setTransitionListener(transitionListener)
        val delay = 5000 //milliseconds

        handlerCall.postDelayed(object : Runnable {
            override fun run() {
                streamResponse = Repository().getListOfStreams()
                streamResponse.observeForever(object : Observer<Resource<List<StreamResponse>>> {
                    override fun onChanged(resource: Resource<List<StreamResponse>>?) {
                        if(resource != null){
                            when(resource.state){
                                State.LOADING->{}
                                State.SUCCESS->{
                                    val list =(resource.data)?.toMutableList()
                                    if(list!= null && list.size >0){
                                        badgeLive.text = "live"
                                        badgeLive.visibility = View.VISIBLE
                                        for (i in 0 until list.size) {
                                            if(!listOfSeenStreams.contains(list[i].streamId)) {
                                                Handler(Looper.getMainLooper()).postDelayed({
                                                    expandableLayout.visibility = View.VISIBLE
                                                    expandableLayout.transitionToEnd()
                                                    tvStreamTitle.text = list[0].streamTitle
                                                    listOfSeenStreams.add(list[i].streamId)
                                                }, i.toLong() * 5000)
                                                Handler(Looper.getMainLooper()).postDelayed({
                                                    expandableLayout.transitionToStart()
                                                }, (i + 1).toLong() * 5000)
                                            }
                                        }
                                    } else {
                                        val seenVideos  = UserCache.newInstance().getSeenVideos(context)
                                        val nonSeenNumber = Repository().getListOfVideos().size  - seenVideos.size
                                        if(nonSeenNumber>0) {
                                            badgeLive.text = nonSeenNumber.toString()
                                            badgeLive.visibility = View.VISIBLE
                                        }else
                                            badgeLive.visibility = View.GONE
                                    }
                                    streamResponse.removeObserver(this)
                                }
                                State.FAILURE->{
                                    BaseViewModel.error.postValue(resource.message)
                                    streamResponse.removeObserver(this)
                                }
                            }
                        }
                    }
                })
                handlerCall.postDelayed(this, delay.toLong())
            }
        }, delay.toLong())
    }

    override fun onStart() {
        AppExecutors()
    }

    override fun onStop() {

    }

}