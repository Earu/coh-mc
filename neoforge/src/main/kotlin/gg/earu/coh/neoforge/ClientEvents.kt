package gg.earu.coh.neoforge

import gg.earu.coh.Coh
import gg.earu.coh.client.ChatPoller
import gg.earu.coh.core.CohConfig
import gg.earu.coh.net.CohPayloads
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.ClientTickEvent

/** Only loaded on the client dist. */
object ClientEvents {
    fun wire() {
        ChatPoller.config = CohConfig.loadClient(Coh.platform.configDir)
        ChatPoller.canSend = {
            Minecraft.getInstance().connection?.hasChannel(CohPayloads.TypingPayload.TYPE) == true
        }
        ChatPoller.sendPayload = { payload ->
            Minecraft.getInstance().connection?.send(ServerboundCustomPayloadPacket(payload))
        }
    }

    @SubscribeEvent
    fun onClientTick(@Suppress("UNUSED_PARAMETER") event: ClientTickEvent.Post) {
        ChatPoller.tick(Minecraft.getInstance())
    }
}
