package gg.earu.coh.client

import gg.earu.coh.core.ClientConfig
import gg.earu.coh.core.TextSanitizer
import gg.earu.coh.net.CohPayloads
import gg.earu.coh.net.CohPayloads.TypingPayload
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.ChatScreen

/**
 * Zero-mixin chat capture: polls the open ChatScreen's EditBox through public API every
 * client tick. Text never leaves the client when it's a command, a hide-prefixed message,
 * or the mod is disabled.
 */
object ChatPoller {
    /** Wired per loader. */
    var sendPayload: (TypingPayload) -> Unit = {}
    var canSend: () -> Boolean = { false }
    var config: ClientConfig = ClientConfig()

    private var open = false
    private var lastSent: String? = null
    private var lastSendMs = 0L

    private const val THROTTLE_MS = 150L

    fun tick(mc: Minecraft) {
        val screen = mc.screen
        val chatOpen = screen is ChatScreen && config.enabled && canSend()

        if (chatOpen && !open) {
            open = true
            lastSent = ""
            lastSendMs = 0
            send(CohPayloads.KIND_START, "")
        } else if (!chatOpen && open) {
            open = false
            send(CohPayloads.KIND_END, "")
        }
        if (!open || screen !is ChatScreen) return

        val raw = screen.children().filterIsInstance<EditBox>().firstOrNull()?.value ?: return
        // Concealed text is replaced by "" (server shows just the typing indicator), matching GMod.
        val effective = if (TextSanitizer.shouldConceal(raw)) "" else raw
        val now = System.currentTimeMillis()
        if (effective != lastSent && now - lastSendMs >= THROTTLE_MS) {
            lastSent = effective
            lastSendMs = now
            send(CohPayloads.KIND_TEXT, effective)
        }
    }

    private fun send(kind: Int, text: String) {
        if (!canSend()) {
            gg.earu.coh.Coh.LOGGER.debug("ChatPoller: cannot send (channel absent)")
            return
        }
        gg.earu.coh.Coh.LOGGER.debug("ChatPoller: send kind={} len={}", kind, text.length)
        sendPayload(TypingPayload(kind, text))
    }
}
