package gg.earu.coh.core

import java.util.UUID

/** Receives display commands from the session manager. Implemented by DisplayManager in game, by fakes in tests. */
interface DisplaySink {
    fun spawn(playerId: UUID, text: String)

    /** Also resets any in-progress fade back to fully opaque. */
    fun update(playerId: UUID, text: String)

    /** progress 0..1 across the fade window at the end of a popup's life. */
    fun fade(playerId: UUID, progress: Float)

    fun remove(playerId: UUID)
}

/**
 * Pure-Kotlin per-player state machine: IDLE → TYPING → (POPUP | IDLE).
 * Time is server ticks (20/s). Mirrors the GMod rtchat/coh behavior:
 * 300 ms server rate limit, 6 s popup lifetime, hide-prefix concealment.
 */
class TypingSessionManager(
    private val configProvider: () -> ServerConfig,
    private val sink: DisplaySink,
) {
    enum class Phase { TYPING, POPUP }

    private class Session(var phase: Phase, var lastActivityTick: Long) {
        var text: String = ""
        var popupStartTick: Long = 0
        var displayed: Boolean = false

        /** Newest text that arrived inside the rate window; flushed by onTick when the gate opens. */
        var pendingText: String? = null
    }

    private val sessions = HashMap<UUID, Session>()
    private val limiter = RateLimiter()

    private val config get() = configProvider()

    val activeIds: Set<UUID> get() = sessions.keys

    fun onStart(id: UUID, nowTick: Long) {
        if (!config.enabled) return
        endInternal(id)
        val session = Session(Phase.TYPING, nowTick)
        sessions[id] = session
        render(id, session)
    }

    fun onText(id: UUID, raw: String, nowTick: Long) {
        if (!config.enabled) return
        val session = sessions[id] ?: return
        if (session.phase != Phase.TYPING) return

        val sanitized = TextSanitizer.sanitize(raw, config.maxChars)
        // Concealment is enforced client-side too; this guards against modified clients.
        val effective = if (TextSanitizer.shouldConceal(sanitized)) "" else sanitized
        session.lastActivityTick = nowTick

        // Rate-limited updates are queued, not dropped — the display always converges to the latest text.
        if (!limiter.tryAcquire(id, nowTick, throttleTicks())) {
            session.pendingText = effective
            return
        }
        applyText(id, session, effective)
    }

    private fun throttleTicks() = (config.throttleMs / 50L).coerceAtLeast(1)

    private fun applyText(id: UUID, session: Session, effective: String) {
        session.pendingText = null
        if (effective == session.text && session.displayed) return
        session.text = effective
        render(id, session)
    }

    fun onEnd(id: UUID, nowTick: Long) {
        val session = sessions[id] ?: return
        // A late END after the chat message converted us to a popup is ignored (ordering race).
        if (session.phase != Phase.TYPING) return
        endInternal(id)
    }

    /** Sent chat message → popup. Works even with no prior session, so vanilla-client typists get popups too. */
    fun onChat(id: UUID, raw: String, nowTick: Long) {
        if (!config.enabled) return
        val sanitized = TextSanitizer.sanitize(raw, config.maxChars)
        if (sanitized.isEmpty() || TextSanitizer.shouldConceal(sanitized)) {
            endInternal(id)
            return
        }
        val session = sessions.getOrPut(id) { Session(Phase.POPUP, nowTick) }
        session.phase = Phase.POPUP
        session.text = sanitized
        session.popupStartTick = nowTick
        session.lastActivityTick = nowTick
        render(id, session)
    }

    fun onTick(nowTick: Long) {
        if (sessions.isEmpty()) return
        val popupTicks = (config.popupSeconds * 20).toLong().coerceAtLeast(1)
        val fadeTicks = FADE_TICKS.coerceAtMost(popupTicks)
        val idleTicks = (config.idleTimeoutSeconds * 20).toLong().coerceAtLeast(1)

        val iterator = sessions.entries.iterator()
        while (iterator.hasNext()) {
            val (id, session) = iterator.next()
            when (session.phase) {
                Phase.TYPING -> {
                    val pending = session.pendingText
                    if (pending != null && limiter.tryAcquire(id, nowTick, throttleTicks())) {
                        applyText(id, session, pending)
                    }
                    if (nowTick - session.lastActivityTick > idleTicks) {
                        if (session.displayed) sink.remove(id)
                        limiter.forget(id)
                        iterator.remove()
                    }
                }

                Phase.POPUP -> {
                    val age = nowTick - session.popupStartTick
                    if (age >= popupTicks) {
                        if (session.displayed) sink.remove(id)
                        limiter.forget(id)
                        iterator.remove()
                    } else if (session.displayed && age >= popupTicks - fadeTicks) {
                        sink.fade(id, (age - (popupTicks - fadeTicks)).toFloat() / fadeTicks)
                    }
                }
            }
        }
    }

    fun onPlayerGone(id: UUID) {
        endInternal(id)
    }

    fun clearAll() {
        for ((id, session) in sessions) {
            if (session.displayed) sink.remove(id)
            limiter.forget(id)
        }
        sessions.clear()
    }

    private fun endInternal(id: UUID) {
        val session = sessions.remove(id) ?: return
        if (session.displayed) sink.remove(id)
        limiter.forget(id)
    }

    private fun render(id: UUID, session: Session) {
        val visible = session.text.isNotEmpty() || (session.phase == Phase.TYPING && config.showTypingIndicator)
        if (visible) {
            if (session.displayed) {
                sink.update(id, session.text)
            } else {
                sink.spawn(id, session.text)
                session.displayed = true
            }
        } else if (session.displayed) {
            sink.remove(id)
            session.displayed = false
        }
    }

    companion object {
        /** GMod fades over the last ~0.7 s of a popup's life. */
        const val FADE_TICKS = 14L
    }
}
