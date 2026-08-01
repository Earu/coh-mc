package gg.earu.coh.neoforge

import gg.earu.coh.Coh
import gg.earu.coh.client.ChatPoller
import gg.earu.coh.core.CohConfig
import net.minecraft.client.Minecraft
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

/** Only loaded on the client dist. */
object ClientEvents {
    fun wire() {
        ChatPoller.config = CohConfig.loadClient(Coh.platform.configDir)
        ChatPoller.canSend = {
            val connection = Minecraft.getInstance().connection
            connection != null && Payloads.channel.isRemotePresent(connection.connection)
        }
        ChatPoller.sendPayload = { Payloads.channel.sendToServer(it) }
    }

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase == TickEvent.Phase.END) ChatPoller.tick(Minecraft.getInstance())
    }
}
