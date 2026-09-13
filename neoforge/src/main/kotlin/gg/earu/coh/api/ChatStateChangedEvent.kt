package gg.earu.coh.api

import net.minecraftforge.eventbus.api.Event
import java.util.UUID

/** Posted on the game bus for every change [ChatOverHead.addListener] would report. Not cancellable. */
class ChatStateChangedEvent(val change: ChatStateChange) : Event() {
    val playerId: UUID get() = change.playerId
    val previous: ChatState get() = change.previous
    val current: ChatState get() = change.current
    val previousMs: Long get() = change.previousMs
}
