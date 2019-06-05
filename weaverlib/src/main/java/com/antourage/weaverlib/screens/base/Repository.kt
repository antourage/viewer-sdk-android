package com.antourage.weaverlib.screens.base

import android.arch.lifecycle.LiveData
import com.antourage.weaverlib.other.firebase.FirestoreDatabase
import com.antourage.weaverlib.other.firebase.QuerySnapshotLiveData
import com.antourage.weaverlib.other.firebase.QuerySnapshotValueLiveData
import com.antourage.weaverlib.other.models.*
import com.antourage.weaverlib.other.networking.ApiClient
import com.antourage.weaverlib.other.networking.base.ApiResponse
import com.antourage.weaverlib.other.networking.base.NetworkBoundResource
import com.antourage.weaverlib.other.networking.base.Resource
import com.google.firebase.firestore.Query
import kotlin.collections.ArrayList


class Repository {

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
//                "https://d33kzx2k689f49.cloudfront.net/out/v1/b8cf10bb32e54a339be195906b6ac165/index.m3u8",
//                baseUrl + "1. The lads are heading out to training.mp4/playlist.m3u8",
                "https://d1my4itlgzru7b.cloudfront.net/out/v1/ff0c96a98d7b44aa8a9a9e3ed1a96dad/index.m3u8",
//                "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
                listOf(),
                30
            )
        )
        list.add(
            StreamResponse(
                2, "Roommates with Head and Shoulders", "Kyle Walker and John Stones", "",
                "file:///android_asset/2.png", baseUrl + "2. Roommates.mp4/playlist.m3u8", listOf(),19
            )
        )
        list.add(
            StreamResponse(
                3, "Live Coverage", "England Women vs Sweden Women", "",
                "file:///android_asset/3.png", baseUrl + "3. England v Sweden LIVE.mp4/playlist.m3u8", listOf(),26
            )
        )
        list.add(
            StreamResponse(
                4, "Stars of the Future with Nike", "U21 5-a-side game", "",
                "file:///android_asset/4.png", baseUrl + "4. England U21 5_a_side.mp4/playlist.m3u8", listOf(),19
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
                listOf(),
                29
            )
        )
        list.add(
            StreamResponse(
                6, "Media Day with M&S", "The squad has been selected", "",
                "file:///android_asset/6.png", baseUrl + "6. Media Day.mp4/playlist.m3u8", listOf(),35
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
                listOf(),
                40
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

    data class MessageEmulation(val timestamp: Int, val nickname: String, val text: String)

     fun getMessagesList(streamId:Int):List<MessageEmulation>{
        val map = getChatMapping()
        return map[streamId]!!
    }
    private fun getChatMapping(): Map<Int, List<MessageEmulation>> {
        return mapOf(
            Pair(
                1, listOf(
                    MessageEmulation(timestamp= 2, nickname= "LondonLion", text= "These lads literally get paid to play football\neveryday…"),
                    MessageEmulation(timestamp = 3, nickname = "Jaybob", text = "Where is this? Where’s the training ground?"),
                    MessageEmulation(timestamp = 7, nickname = "ChelskiChris", text = "This is pretty cool, why hasn’t this been possible forever?"),
                    MessageEmulation(timestamp = 8, nickname = "TheGunnar", text = "The Spurs lads look like a bunch of fun 🤔"),
                    MessageEmulation(timestamp = 9, nickname = "LondonLion", text = "St. George’s Park I think Jaybob"),
                    MessageEmulation(timestamp = 16, nickname = "Jaybob", text = "Not far from me!"),
                    MessageEmulation(timestamp = 18, nickname = "SteveTheSpur", text = "haha"),
                    MessageEmulation(timestamp = 22, nickname = "TheGunnar", text = "A spurs fan would think that’s funny!"),
                    MessageEmulation(timestamp = 24, nickname = "WelcometoManc", text = "Look like pretty good mates?!??! Maybe…."),
                    MessageEmulation(timestamp = 26, nickname = "ChelskiChris", text = "At least they’re taking it seriously…"),
                    MessageEmulation(timestamp = 27, nickname = "SteveTheSpur", text = "No chance Welcome to Manc"),
                    MessageEmulation(timestamp = 28, nickname = "WelcometoManc", text = "That’s how I would behave. Enjoy right!")
                )
            ),
            Pair(2, listOf(
                    MessageEmulation(timestamp= 1, nickname="CityBlue", text= "I actually love these boys, seem like normal lads!"),
                    MessageEmulation(timestamp= 2, nickname="ChelskiChris", text= "Kyle Walker or Alexander-Arnold?"),
                    MessageEmulation(timestamp= 3, nickname="RedDevil", text= "Yeah they do seem alright. Hurts to say."),
                    MessageEmulation(timestamp= 5, nickname="TheGunnar", text= "Hmmmmm Can I vote for Lee Dixon?"),
                    MessageEmulation(timestamp= 7, nickname="LondonLion", text= "AA is a much smarter footballer"),
                    MessageEmulation(timestamp= 10, nickname= "JoeyJim", text= "Not sure now, but in a few years it will be AA"),
                    MessageEmulation(timestamp= 11, nickname= "SteveTheSpur", text= "I just wish Robertson was English."),
                    MessageEmulation(timestamp= 13, nickname= "TheGunnar", text= "Roommates is the best show on here."),
                    MessageEmulation(timestamp= 14, nickname= "BrexitBaby", text= "Nah Kyle is much more reliable at the back!"),
                    MessageEmulation(timestamp= 17, nickname= "SteveTheSpur", text= "Not sure about that JoeyJim. Even as a spurts fan."),
                    MessageEmulation(timestamp= 18, nickname= "ChelskiChris", text= "hahahhahaha Hilarious")
                )
            ),
            Pair(3, listOf(
                    MessageEmulation(timestamp= 1, nickname="SuperLion", text= "They’re struggling"),
                    MessageEmulation(timestamp= 3, nickname="StephHoughtonNo1", text= "It’s gonna be tough from here."),
                    MessageEmulation(timestamp= 4, nickname="SteveTheSpur", text= "Credit where it’s due, the Swede’s are pretty good."),
                    MessageEmulation(timestamp= 5, nickname="GottaBelieve", text= "Come on England!!!!!"),
                    MessageEmulation(timestamp= 8, nickname="StephHoughtonNo1", text= "Agreed SteveTheSpur, well organised."),
                    MessageEmulation(timestamp= 9, nickname="StephHoughtonNo1", text= "😔"),
                    MessageEmulation(timestamp= 13, nickname= "SuperLion", text= "GottaBelieve, Gotta Believe!"),
                    MessageEmulation(timestamp= 18, nickname= "TheGunnar", text= " StephHoughtonNo1, have you seen Steph on Roommates?"),
                    MessageEmulation(timestamp= 22, nickname= "TheGunnar", text= "Roommates is the best show on here, I say that all the time"),
                    MessageEmulation(timestamp= 24, nickname= "BrexitBaby", text= "Loads of time left to play."),
                    MessageEmulation(timestamp= 25, nickname= "Jaybob", text= "I fancy the Swedes to do well in the next few tournaments."),
                    MessageEmulation(timestamp= 26, nickname= "LondonLion", text= "Good finish")
                )
            ),
            Pair(4, listOf(
                    MessageEmulation(timestamp= 2, nickname= "RedDevil", text= "These boys are pretty good!"),
                    MessageEmulation(timestamp= 3, nickname= "StephHoughtonNo1", text= "They’re not holding back!!!!"),
                    MessageEmulation(timestamp= 4, nickname= "UptheReds", text= "Liverpool have got so many good young players coming through"),
                    MessageEmulation(timestamp= 5, nickname= "LondonLion", text= "Skillllzzzzzz"),
                    MessageEmulation(timestamp= 8, nickname= "Jaybob", text= "This is better than watching an actual game"),
                    MessageEmulation(timestamp= 9, nickname= "StephHoughtonNo1", text= "😔"),
                    MessageEmulation(timestamp= 13, nickname= "TheGunnar", text= "Agreed, I don’t know where the Scousers are getting them from."),
                    MessageEmulation(timestamp= 14, nickname= "BrexitBaby", text= "I like this, full on training, no holding back"),
                    MessageEmulation(timestamp= 16, nickname= "RedDevil", text= "When is the next U21 comp?"),
                    MessageEmulation(timestamp= 18, nickname= "LondonLion", text= "This summer")
                )
            ),
            Pair(5, listOf(
                    MessageEmulation(timestamp= 1, nickname="CityBlue", text= "OH MY GOD"),
                    MessageEmulation(timestamp= 3, nickname="ChelskiChris", text= "Is this going to happen?"),
                    MessageEmulation(timestamp= 4, nickname="RedDevil", text= "COME ON ENGLAND!!!!!"),
                    MessageEmulation(timestamp= 8, nickname="TheGunnar", text= "This is so England"),
                    MessageEmulation(timestamp= 9, nickname="LondonLion", text= " Positivity, positivity, positivity."),
                    MessageEmulation(timestamp= 16, nickname= "JoeyJim", text= "Eric Dier 🙈 🙈 🙈"),
                    MessageEmulation(timestamp= 18, nickname= "SteveTheSpur", text= "It’s coming home!!!!!"),
                    MessageEmulation(timestamp= 22, nickname= "TheGunnar", text= "FOOTBALL IS COMING HOME"),
                    MessageEmulation(timestamp= 24, nickname= "BrexitBaby", text= " THIS IS IT!!!!"),
                    MessageEmulation(timestamp= 25, nickname= "SteveTheSpur", text= "OH MY GOD AGAIN"),
                    MessageEmulation(timestamp= 27, nickname= "ChelskiChris", text= "I need a nap.")
                )
            ),
            Pair(6, listOf(
                    MessageEmulation(timestamp= 1, nickname="UptheReds", text= "JLing, JLing, JLing, JLing, JLing, JLing, JLing"),
                    MessageEmulation(timestamp= 3, nickname="OOOHAAAAH….", text= "Love him and his mum at the world cup"),
                    MessageEmulation(timestamp= 7, nickname="RedDevil", text= "He knows who he’s gotta get… I think they’re mates too."),
                    MessageEmulation(timestamp= 8, nickname="BigOleGunnar", text= "It has all been spurs"),
                    MessageEmulation(timestamp= 9, nickname="LondonLion", text= "AA is a much smarter footballer"),
                    MessageEmulation(timestamp= 13, nickname= "BrexitBaby", text= "Surely the reporters can’t be happy!"),
                    MessageEmulation(timestamp= 18, nickname= "SteveTheSpur", text= "England is all Spurs BigOleGunnar 😎"),
                    MessageEmulation(timestamp= 22, nickname= "TheGunnar", text= "More roommates!!!!!"),
                    MessageEmulation(timestamp= 24, nickname= "BrexitBaby", text= "Looks pretty happy to be rescued."),
                    MessageEmulation(timestamp= 26, nickname= "SteveTheSpur", text= "Not sure about that JoeyJim. Even as a spurts fan."),
                    MessageEmulation(timestamp= 30, nickname= "ChelskiChris", text= "Still not sure Jesse will be at United longterm. Chelsea will have him!"),
                    MessageEmulation(timestamp= 34, nickname= "SteveTheSpur", text= "This is on YouTube too?")
                )
            ),
            Pair(7, listOf(
                MessageEmulation(timestamp= 1, nickname= "BringBackShearer", text= "How do I get this job?"),
                MessageEmulation(timestamp= 3, nickname= "ChelskiChris", text= "Rach Stringer is pretty cool!"),
                MessageEmulation(timestamp= 4, nickname= "GottaBelieve", text= "Do you find yourself watching this instead of the footy?"),
                MessageEmulation(timestamp= 5, nickname= "TheGunnar", text= "I remember when Arsenal used to have a few England players 😴"),
                MessageEmulation(timestamp= 8, nickname= "StephHoughtonNo1", text= "This is me, I’m gonna win this.."),
                MessageEmulation(timestamp= 9, nickname="JoeyJim", text= "Really worried about fatigue here"),
                MessageEmulation(timestamp= 14, nickname= "SuperLion", text= "Where’s Gotta Believe when you need them?!"),
                MessageEmulation(timestamp= 21, nickname= "TheGunnar", text= "Have I mentioned Roommates?"),
                MessageEmulation(timestamp= 24, nickname= "TheGunnar", text= "Roommates is the best show on here, I say that all the time"),
                MessageEmulation(timestamp= 27, nickname= "SteveTheSpur", text= "I know this, I think I do know, I think"),
                MessageEmulation(timestamp= 32, nickname= "Jaybob", text= "They watch football for a living!!??!?!?!"),
                MessageEmulation(timestamp= 35, nickname= "RedDevil", text= "I watched that show, can’t remember")
            ))
        )
    }

    //region Firebase
    fun addMessage(message: Message,streamId:Int){
        FirestoreDatabase().getMessagesReferences(streamId).document().set(message)
    }
    fun getMessages(streamId: Int):QuerySnapshotLiveData<Message>{
        return QuerySnapshotLiveData(FirestoreDatabase().getMessagesReferences(streamId).orderBy("timestamp",
            Query.Direction.ASCENDING),Message::class.java)
    }
    fun getStreamLiveData(streamId: Int): QuerySnapshotValueLiveData<Stream>{
        val docRef = FirestoreDatabase().getStreamsCollection().document(streamId.toString())
        return QuerySnapshotValueLiveData(docRef, Stream::class.java)
    }
    //endregion
}