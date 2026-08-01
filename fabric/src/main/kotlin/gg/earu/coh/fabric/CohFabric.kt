package gg.earu.coh.fabric

import gg.earu.coh.Coh
import gg.earu.coh.net.CohPayloads
import gg.earu.coh.platform.Platform
import gg.earu.coh.server.CohServer
import net.fabricmc.api.EnvType
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Path

class CohFabric : ModInitializer {
    class FabricPlatform : Platform {
        override val configDir: Path = FabricLoader.getInstance().configDir.resolve(Coh.MOD_ID)
        override val isClient: Boolean = FabricLoader.getInstance().environmentType == EnvType.CLIENT
        override val modVersion: String = FabricLoader.getInstance()
            .getModContainer(Coh.MOD_ID).map { it.metadata.version.friendlyString }.orElse("dev")
    }

    override fun onInitialize() {
        Coh.init(FabricPlatform())
        CohServer.init(Coh.platform)

        PayloadTypeRegistry.playC2S().register(CohPayloads.TypingPayload.TYPE, CohPayloads.TypingPayload.CODEC)

        ServerPlayNetworking.registerGlobalReceiver(CohPayloads.TypingPayload.TYPE) { payload, context ->
            context.server().execute { CohServer.onTyping(context.player(), payload) }
        }

        ServerMessageEvents.CHAT_MESSAGE.register { message, sender, _ ->
            CohServer.onChat(sender, message.signedContent())
        }

        ServerTickEvents.END_SERVER_TICK.register { CohServer.onTick() }
        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ -> CohServer.onPlayerLeave(handler.player) }
        ServerLifecycleEvents.SERVER_STARTED.register { CohServer.onServerStarted(it) }
        ServerLifecycleEvents.SERVER_STOPPING.register { CohServer.onServerStopping(it) }
    }
}
