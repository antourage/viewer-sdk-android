package com.antourage.weaverlib.screens.weaver

import com.antourage.weaverlib.other.models.Poll

internal sealed class PollStatus {
    object NoPoll : PollStatus()
    class ActivePoll(val poll: Poll) : PollStatus()
    class ActivePollDismissed(val pollStatus: String?) : PollStatus()
    class PollDetails(val pollId: String) : PollStatus()
}