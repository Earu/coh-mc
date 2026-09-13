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
        // Forge 47 fires this from the chat decorator on a worker thread; the session map is server-thread only.
        val player = event.player
        val text = event.rawText
        player.server.execute { CohServer.onChat(player, text) }
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
