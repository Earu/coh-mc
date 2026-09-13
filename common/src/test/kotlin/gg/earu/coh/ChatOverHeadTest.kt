package gg.earu.coh

import gg.earu.coh.api.ChatOverHead
import gg.earu.coh.api.ChatState
import gg.earu.coh.api.ChatStateChange
import gg.earu.coh.api.ChatStateListener
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatOverHeadTest {
    private val id = UUID.randomUUID()
    private val received = mutableListOf<ChatStateChange>()
    private val listener = ChatStateListener { received += it }

    @AfterTest
    fun tearDown() {
        ChatOverHead.removeListener(listener)
        ChatOverHead.record(id, ChatOverHead.stateOf(id), ChatState.IDLE)
    }

    @Test
    fun `unknown players are idle`() {
        assertEquals(ChatState.IDLE, ChatOverHead.stateOf(id))
        assertEquals(0L, ChatOverHead.millisInStateOf(id))
        assertTrue(id !in ChatOverHead.activePlayers())
    }

    @Test
    fun `view follows recorded transitions and listeners hear real changes only`() {
        ChatOverHead.addListener(listener)
        ChatOverHead.record(id, ChatState.IDLE, ChatState.TYPING)
        assertEquals(ChatState.TYPING, ChatOverHead.stateOf(id))
        assertEquals(mapOf(id to ChatState.TYPING), ChatOverHead.activePlayers().filterKeys { it == id })

        ChatOverHead.record(id, ChatState.TYPING, ChatState.POPUP)
        ChatOverHead.record(id, ChatState.POPUP, ChatState.POPUP)
        ChatOverHead.record(id, ChatState.POPUP, ChatState.IDLE)
        assertEquals(ChatState.IDLE, ChatOverHead.stateOf(id))

        assertEquals(
            listOf(ChatState.IDLE to ChatState.TYPING, ChatState.TYPING to ChatState.POPUP, ChatState.POPUP to ChatState.IDLE),
            received.map { it.previous to it.current },
        )
        assertEquals(0L, received.first().previousMs)
    }

    @Test
    fun `a throwing listener does not block the others`() {
        val bad = ChatStateListener { throw IllegalStateException("boom") }
        ChatOverHead.addListener(bad)
        ChatOverHead.addListener(listener)
        try {
            ChatOverHead.record(id, ChatState.IDLE, ChatState.TYPING)
        } finally {
            ChatOverHead.removeListener(bad)
        }
        assertEquals(1, received.size)
    }
}
