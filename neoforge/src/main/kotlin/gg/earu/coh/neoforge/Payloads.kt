package gg.earu.coh.neoforge

import gg.earu.coh.net.CohPayloads
import gg.earu.coh.server.CohServer
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.network.NetworkEvent
import net.minecraftforge.network.NetworkRegistry
import java.util.function.Supplier

/**
 * Forge 1.20.1 SimpleChannel wiring for the typing message. The channel is OPTIONAL
 * (acceptMissingOr) so vanilla clients/servers interoperate untouched.
 */
object Payloads {
    private const val PROTOCOL = "1"

    val channel = NetworkRegistry.ChannelBuilder
        .named(ResourceLocation(gg.earu.coh.Coh.MOD_ID, CohPayloads.TYPING_PATH))
        .networkProtocolVersion { PROTOCOL }
        .clientAcceptedVersions(NetworkRegistry.acceptMissingOr(PROTOCOL))
        .serverAcceptedVersions(NetworkRegistry.acceptMissingOr(PROTOCOL))
        .simpleChannel()

    fun register() {
        channel.registerMessage(
            0,
            CohPayloads.TypingPayload::class.java,
            { msg, buf -> buf.writeVarInt(msg.kind); buf.writeUtf(msg.text, CohPayloads.MAX_TEXT) },
            { buf -> CohPayloads.TypingPayload(buf.readVarInt(), buf.readUtf(CohPayloads.MAX_TEXT)) },
            ::handleTyping,
        )
    }

    private fun handleTyping(msg: CohPayloads.TypingPayload, ctx: Supplier<NetworkEvent.Context>) {
        val player = ctx.get().sender
        if (player != null) {
            ctx.get().enqueueWork { CohServer.onTyping(player, msg) }
        }
        ctx.get().packetHandled = true
    }
}
