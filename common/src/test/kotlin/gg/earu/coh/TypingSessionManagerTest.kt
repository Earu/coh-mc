package gg.earu.coh

import gg.earu.coh.core.DisplaySink
import gg.earu.coh.core.ServerConfig
import gg.earu.coh.core.TypingSessionManager
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeSink : DisplaySink {
    val calls = mutableListOf<String>()
    override fun spawn(playerId: UUID, text: String) { calls += "spawn:$text" }
    override fun update(playerId: UUID, text: String) { calls += "update:$text" }
    override fun fade(playerId: UUID, progress: Float) { calls += "fade" }
    override fun freeze(playerId: UUID) { calls += "freeze" }
    override fun remove(playerId: UUID) { calls += "remove" }
    fun last() = calls.lastOrNull()
}

class TypingSessionManagerTest {
    private val id = UUID.randomUUID()

    private fun manager(config: ServerConfig = ServerConfig()): Pair<TypingSessionManager, FakeSink> {
        val sink = FakeSink()
        return TypingSessionManager({ config }, sink) to sink
    }

    @Test
    fun `start spawns typing indicator`() {
        val (m, sink) = manager()
        m.onStart(id, 0)
        assertEquals(listOf("spawn:"), sink.calls)
    }

    @Test
    fun `start without indicator spawns nothing until text arrives`() {
        val (m, sink) = manager(ServerConfig(showTypingIndicator = false))
        m.onStart(id, 0)
        assertEquals(emptyList(), sink.calls)
        m.onText(id, "hey", 10)
        assertEquals(listOf("spawn:hey"), sink.calls)
    }

    @Test
    fun `text updates the display`() {
        val (m, sink) = manager()
        m.onStart(id, 0)
        m.onText(id, "hello", 10)
        assertEquals("update:hello", sink.last())
    }

    @Test
    fun `rate limited updates queue and flush on tick`() {
        val (m, sink) = manager() // 150 ms = 3 ticks
        m.onStart(id, 0)
        m.onText(id, "a", 10)
        assertEquals("update:a", sink.last())
        m.onText(id, "ab", 11) // inside the window -> queued, not shown yet
        m.onText(id, "abc", 12) // newer text replaces the queued one
        assertEquals("update:a", sink.last())
        m.onTick(13) // gate opens -> newest text flushes
        assertEquals("update:abc", sink.last())
    }

    @Test
    fun `hidden and command text conceal to indicator`() {
        val (m, sink) = manager()
        m.onStart(id, 0)
        m.onText(id, "-- hide secret", 10)
        assertEquals("spawn:", sink.calls.last()) // still just the indicator, never updated with text
        m.onText(id, "/tp secret", 20)
        assertEquals(1, sink.calls.size) // "" == "" -> no redundant update
    }

    @Test
    fun `end removes the display and late end after chat is ignored`() {
        val (m, sink) = manager()
        m.onStart(id, 0)
        m.onText(id, "hello", 10)
        m.onChat(id, "hello", 20) // converts to popup
        m.onEnd(id, 21) // late END from client -> ignored
        assertTrue(sink.calls.none { it == "remove" })
        assertTrue(id in m.activeIds)
    }

    @Test
    fun `end during typing removes`() {
        val (m, sink) = manager()
        m.onStart(id, 0)
        m.onEnd(id, 5)
        assertEquals("remove", sink.last())
        assertTrue(m.activeIds.isEmpty())
    }

    @Test
    fun `popup expires after lifetime with fade near the end`() {
        val (m, sink) = manager() // 6 s = 120 ticks
        m.onChat(id, "gg", 0) // vanilla typist: popup without prior session
        assertEquals(listOf("spawn:gg", "freeze"), sink.calls)
        m.onTick(100)
        assertTrue(sink.calls.none { it == "fade" })
        m.onTick(110) // inside the 14-tick fade window
        assertEquals("fade", sink.last())
        m.onTick(120)
        assertEquals("remove", sink.last())
        assertTrue(m.activeIds.isEmpty())
    }

    @Test
    fun `sending freezes the popup where it was sent`() {
        val (m, sink) = manager()
        m.onStart(id, 0)
        m.onText(id, "hell", 10)
        m.onChat(id, "hello", 20)
        assertEquals(listOf("spawn:", "update:hell", "update:hello", "freeze"), sink.calls)
    }

    @Test
    fun `a second message starts a new popup instead of retexting the frozen one`() {
        val (m, sink) = manager()
        m.onChat(id, "one", 0)
        sink.calls.clear()
        m.onChat(id, "two", 40)
        assertEquals(listOf("remove", "spawn:two", "freeze"), sink.calls)
    }

    @Test
    fun `typing session times out when idle`() {
        val (m, sink) = manager() // 15 s = 300 ticks
        m.onStart(id, 0)
        m.onTick(200)
        assertTrue(m.activeIds.isNotEmpty())
        m.onTick(301)
        assertEquals("remove", sink.last())
        assertTrue(m.activeIds.isEmpty())
    }

    @Test
    fun `disabled config does nothing`() {
        val (m, sink) = manager(ServerConfig(enabled = false))
        m.onStart(id, 0)
        m.onText(id, "hello", 10)
        m.onChat(id, "hello", 20)
        assertEquals(emptyList(), sink.calls)
    }

    @Test
    fun `concealed chat kills any live display`() {
        val (m, sink) = manager()
        m.onStart(id, 0)
        m.onChat(id, "-- hide whisper", 10)
        assertEquals("remove", sink.last())
        assertTrue(m.activeIds.isEmpty())
    }
}
