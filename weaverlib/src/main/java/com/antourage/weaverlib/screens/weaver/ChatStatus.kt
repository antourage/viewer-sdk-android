package com.antourage.weaverlib.screens.weaver

sealed class ChatStatus {
    object ChatTurnedOff : ChatStatus()
    object ChatNoMessages : ChatStatus()
    object ChatMessages : ChatStatus()
}