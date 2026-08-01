package gg.earu.coh.neoforge

import gg.earu.coh.server.CohServer
import net.minecraft.server.level.ServerPlayer
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.ServerChatEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.server.ServerStartedEvent
import net.neoforged.neoforge.event.server.ServerStoppingEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent

object ServerEvents {
    @SubscribeEvent
    fun onServerChat(event: ServerChatEvent) {
        CohServer.onChat(event.player, event.rawText)
    }

    @SubscribeEvent
    fun onServerTick(@Suppress("UNUSED_PARAMETER") event: ServerTickEvent.Post) {
        CohServer.onTick()
    }

    @SubscribeEvent
    fun onPlayerLoggedOut(event: PlayerEvent.PlayerLoggedOutEvent) {
        (event.entity as? ServerPlayer)?.let { CohServer.onPlayerLeave(it) }
    }

    @SubscribeEvent
    fun onServerStarted(event: ServerStartedEvent) {
        CohServer.onServerStarted(event.server)
    }

    @SubscribeEvent
    fun onServerStopping(event: ServerStoppingEvent) {
        CohServer.onServerStopping(event.server)
    }
}
