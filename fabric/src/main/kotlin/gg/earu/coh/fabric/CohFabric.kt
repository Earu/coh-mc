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
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.ResourceLocation
import java.nio.file.Path

/** Channel id for the 1.20.1 raw-channel networking (pre-payload-types API). */
object FabricChannel {
    val TYPING = ResourceLocation(Coh.MOD_ID, CohPayloads.TYPING_PATH)
}

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

        ServerPlayNetworking.registerGlobalReceiver(FabricChannel.TYPING) { server, player, _, buf, _ ->
            val kind = buf.readVarInt()
            val text = buf.readUtf(CohPayloads.MAX_TEXT)
            // Raw-channel handlers run on netty threads; hop to the server thread.
            server.execute { CohServer.onTyping(player, CohPayloads.TypingPayload(kind, text)) }
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
