package gg.earu.coh.fabric

import gg.earu.coh.Coh
import gg.earu.coh.client.ChatPoller
import gg.earu.coh.core.CohConfig
import gg.earu.coh.net.CohPayloads
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.client.Minecraft

class CohFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
        ChatPoller.config = CohConfig.loadClient(Coh.platform.configDir)
        ChatPoller.canSend = {
            Minecraft.getInstance().connection != null && ClientPlayNetworking.canSend(FabricChannel.TYPING)
        }
        ChatPoller.sendPayload = { payload ->
            val buf = PacketByteBufs.create()
            buf.writeVarInt(payload.kind)
            buf.writeUtf(payload.text, CohPayloads.MAX_TEXT)
            ClientPlayNetworking.send(FabricChannel.TYPING, buf)
        }

        ClientTickEvents.END_CLIENT_TICK.register { ChatPoller.tick(it) }
    }
}
