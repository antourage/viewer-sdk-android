package com.antourage.weaverlib.screens.base

import android.arch.lifecycle.LiveData
import com.antourage.weaverlib.other.models.Poll
import com.antourage.weaverlib.other.models.PollAnswers
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.ApiClient
import com.antourage.weaverlib.other.networking.base.ApiResponse
import com.antourage.weaverlib.other.networking.base.NetworkBoundResource
import com.antourage.weaverlib.other.networking.base.Resource


class Repository() {

    private var currentPoll: Poll

    init {
        val answers = ArrayList<PollAnswers>()
        val list = ArrayList<String>()
        list.add("ivan")
        list.add("natalia")
        answers.add(PollAnswers("yes i like it a lot", list))
        answers.add(PollAnswers("it is OK", ArrayList()))
        list.add("inna")
        answers.add(PollAnswers("bad choice", list))
        answers.add(PollAnswers("I am not sure", list))
        currentPoll = Poll(
            1, 2, 3, 0L,
            "What do you think about eurovision winner?", true, 0L, "", answers
        )
    }

    fun getListOfStreams(): LiveData<Resource<List<StreamResponse>>> {
        return object : NetworkBoundResource<List<StreamResponse>>() {
            override fun saveCallResult(item: List<StreamResponse>) {
            }

            override fun createCall(): LiveData<ApiResponse<List<StreamResponse>>> {
                return ApiClient.getInitialClient().webService.getLiveStreams()
            }
        }.asLiveData()
    }

    fun getListOfVideos(): List<StreamResponse> {
        val list = mutableListOf<StreamResponse>()
        val baseUrl = "http://18.185.126.11:1935/vod/"
        list.add(
            StreamResponse(
                1,
                "Inside England, with Vauxhall",
                "Heading Out for Training",
                "",
                "file:///android_asset/1.png",
                baseUrl + "1. The lads are heading out to training.mp4/playlist.m3u8",
                listOf()
            )
        )
        list.add(
            StreamResponse(
                2, "Roommates with Head and Shoulders", "Kyle Walker and John Stones", "",
                "file:///android_asset/2.png", baseUrl + "2. Roommates.mp4/playlist.m3u8", listOf()
            )
        )
        list.add(
            StreamResponse(
                3, "Live Coverage", "England Women vs Sweden Women", "",
                "file:///android_asset/3.png", baseUrl + "3. England v Sweden LIVE.mp4/playlist.m3u8", listOf()
            )
        )
        list.add(
            StreamResponse(
                4, "Stars of the Future with Nike", "U21 5-a-side game", "",
                "file:///android_asset/4.png", baseUrl + "4. England U21 5_a_side.mp4/playlist.m3u8", listOf()
            )
        )
        list.add(
            StreamResponse(
                5,
                "SupporterReporter",
                "Penalties vs Columbia",
                "",
                "file:///android_asset/5.png",
                baseUrl + "5. %23SupporterReporter vs Columbia.mp4/playlist.m3u8",
                listOf()
            )
        )
        list.add(
            StreamResponse(
                6, "Media Day with M&S", "The squad has been selected", "",
                "file:///android_asset/6.png", baseUrl + "6. Media Day.mp4/playlist.m3u8", listOf()
            )
        )
        list.add(
            StreamResponse(
                7,
                "Bud Light’s Alternative Commentary",
                "FT Reev and Theo Baker",
                "",
                "file:///android_asset/7.png",
                baseUrl + "7. Alternative Commentary, FT Reev and Theo Baker.mp4/playlist.m3u8",
                listOf()
            )
        )

        return list
    }
    fun getCurrentPoll(): Poll {
        val answers = ArrayList<PollAnswers>()
        val list = ArrayList<String>()
        //list.add("ivan");
        //list.add("natalia");
        answers.add(PollAnswers("yes i like it a lot", ArrayList()))
        answers.add(PollAnswers("it is OK", ArrayList()))
        //list.add("inna");
        answers.add(PollAnswers("bad choice", ArrayList()))
        answers.add(PollAnswers("I am not sure", ArrayList()))
        return Poll(
            1, 2, 3, 0L,
            "What do you think about eurovision winner?", true, 0L, "", answers
        )
    }

    fun setCurrentPoll(poll: Poll) {
        currentPoll = poll
    }
}