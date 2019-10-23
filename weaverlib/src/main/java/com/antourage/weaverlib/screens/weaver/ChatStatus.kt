package com.antourage.weaverlib.screens.weaver

internal sealed class ChatStatus {
    object ChatTurnedOff : ChatStatus()
    object ChatNoMessages : ChatStatus()
    object ChatMessages : ChatStatus()
}