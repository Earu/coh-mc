package gg.earu.coh.api

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory

/** Fabric event mirroring [ChatOverHead.addListener]. Same payload, loader-native registration. */
object CohEvents {
    @JvmField
    val STATE_CHANGE: Event<ChatStateListener> = EventFactory.createArrayBacked(ChatStateListener::class.java) { listeners ->
        ChatStateListener { change -> for (listener in listeners) listener.onChatStateChange(change) }
    }
}
