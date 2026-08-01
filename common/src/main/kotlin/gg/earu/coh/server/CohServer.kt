package gg.earu.coh.server

import gg.earu.coh.core.CohConfig
import gg.earu.coh.core.ServerConfig
import gg.earu.coh.core.TypingSessionManager
import gg.earu.coh.net.CohPayloads
import gg.earu.coh.platform.Platform
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

/** Loader-agnostic server hub; loader entrypoints funnel their events here. */
object CohServer {
    lateinit var config: ServerConfig
        private set

    private lateinit var displays: DisplayManager
    private lateinit var sessions: TypingSessionManager
    private var tick = 0L

    fun init(platform: Platform) {
        config = CohConfig.loadServer(platform.configDir)
        displays = DisplayManager { config }
        sessions = TypingSessionManager({ config }, displays)
    }

    fun onServerStarted(server: MinecraftServer) {
        displays.attach(server)
        displays.sweepOrphans(server)
    }

    fun onServerStopping(server: MinecraftServer) {
        sessions.clearAll()
        displays.detach()
    }

    /** Must be called on the server thread. */
    fun onTyping(player: ServerPlayer, payload: CohPayloads.TypingPayload) {
        when (payload.kind) {
            CohPayloads.KIND_START -> sessions.onStart(player.uuid, tick)
            CohPayloads.KIND_TEXT -> sessions.onText(player.uuid, payload.text, tick)
            CohPayloads.KIND_END -> sessions.onEnd(player.uuid, tick)
        }
    }

    fun onChat(player: ServerPlayer, message: String) {
        sessions.onChat(player.uuid, message, tick)
    }

    fun onTick() {
        tick++
        sessions.onTick(tick)
        displays.tick()
    }

    fun onPlayerLeave(player: ServerPlayer) {
        sessions.onPlayerGone(player.uuid)
    }
}
