package gg.earu.coh.neoforge

import gg.earu.coh.server.CohServer
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.event.ServerChatEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.server.ServerStartedEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

object ServerEvents {
    @SubscribeEvent
    fun onServerChat(event: ServerChatEvent) {
        CohServer.onChat(event.player, event.rawText)
    }

    @SubscribeEvent
    fun onServerTick(event: TickEvent.ServerTickEvent) {
        if (event.phase == TickEvent.Phase.END) CohServer.onTick()
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
