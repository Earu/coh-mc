package gg.earu.coh.neoforge

import gg.earu.coh.Coh
import gg.earu.coh.net.CohPayloads
import gg.earu.coh.server.CohServer
import net.minecraft.server.level.ServerPlayer
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent

@Mod(Coh.MOD_ID)
class CohNeoForge(container: ModContainer, modBus: IEventBus) {
    init {
        Coh.init(
            NeoForgePlatform(
                configDir = FMLPaths.CONFIGDIR.get().resolve(Coh.MOD_ID),
                isClient = FMLEnvironment.getDist().isClient,
                modVersion = container.modInfo.version.toString(),
            )
        )
        CohServer.init(Coh.platform)

        modBus.register(ModBusEvents)
        NeoForge.EVENT_BUS.register(ServerEvents)
        if (FMLEnvironment.getDist().isClient) {
            ClientEvents.wire()
            NeoForge.EVENT_BUS.register(ClientEvents)
        }
    }

    object ModBusEvents {
        @SubscribeEvent
        fun onRegisterPayloads(event: RegisterPayloadHandlersEvent) {
            // Optional channel: vanilla clients connect untouched, viewers never need the mod.
            event.registrar("1").optional().playToServer(
                CohPayloads.TypingPayload.TYPE,
                CohPayloads.TypingPayload.CODEC,
            ) { payload, context ->
                val player = context.player() as? ServerPlayer ?: return@playToServer
                context.enqueueWork { CohServer.onTyping(player, payload) }
            }
        }
    }
}
