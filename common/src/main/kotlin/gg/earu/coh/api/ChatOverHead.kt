package gg.earu.coh.api

import gg.earu.coh.Coh
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/** IDLE: nothing over their head. TYPING: chat box open. POPUP: a sent message is still showing. */
enum class ChatState { IDLE, TYPING, POPUP }

/**
 * One transition. [previousMs] is how long the player spent in [previous], 0 when it was IDLE.
 * Fires on the server thread, only when the state actually changes.
 */
data class ChatStateChange(
    val playerId: UUID,
    val previous: ChatState,
    val current: ChatState,
    val previousMs: Long,
)

fun interface ChatStateListener {
    fun onChatStateChange(change: ChatStateChange)
}

/**
 * Server side only. Safe to call at any time: before the server starts or with COH disabled
 * every player is IDLE and [activePlayers] is empty. Never exposes the text being typed.
 *
 * Typing with the hide prefix still counts as TYPING; `showTypingIndicator` only affects rendering.
 * A second message while a popup is up refreshes the popup: no event, the timer restarts.
 * Every path out of a state reports a transition: idle timeout, popup expiry, chat sent with a hide
 * prefix or as a command, player leaving, server stopping.
 *
 * Fabric mirrors listeners on `CohEvents.STATE_CHANGE`, Forge posts `ChatStateChangedEvent`.
 */
object ChatOverHead {

    private class Entry(val state: ChatState, val sinceMs: Long)

    private val entries = ConcurrentHashMap<UUID, Entry>()
    private val listeners = CopyOnWriteArrayList<ChatStateListener>()

    @JvmStatic
    fun stateOf(playerId: UUID): ChatState = entries[playerId]?.state ?: ChatState.IDLE

    /** Milliseconds in the current state, 0 when IDLE. Wall clock, unaffected by TPS. */
    @JvmStatic
    fun millisInStateOf(playerId: UUID): Long =
        entries[playerId]?.let { System.currentTimeMillis() - it.sinceMs } ?: 0L

    /** Everyone not IDLE. A snapshot, safe to keep or iterate. */
    @JvmStatic
    fun activePlayers(): Map<UUID, ChatState> = entries.mapValues { it.value.state }

    @JvmStatic
    fun addListener(listener: ChatStateListener) {
        listeners.add(listener)
    }

    @JvmStatic
    fun removeListener(listener: ChatStateListener) {
        listeners.remove(listener)
    }

    /** Session manager hook. Stamps the transition and dispatches; a popup refresh restarts the clock without an event. */
    internal fun record(playerId: UUID, previous: ChatState, current: ChatState) {
        val now = System.currentTimeMillis()
        val old = if (current == ChatState.IDLE) entries.remove(playerId) else entries.put(playerId, Entry(current, now))
        if (previous == current) return
        val previousMs = if (previous == ChatState.IDLE || old == null) 0L else now - old.sinceMs
        val change = ChatStateChange(playerId, previous, current, previousMs)
        for (listener in listeners) {
            try {
                listener.onChatStateChange(change)
            } catch (e: Exception) {
                Coh.LOGGER.error("ChatOverHead listener threw", e)
            }
        }
    }
}
