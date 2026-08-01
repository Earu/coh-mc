package gg.earu.coh.net

import gg.earu.coh.Coh
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

/**
 * Wire format shared by every loader. The channel is OPTIONAL: vanilla clients/servers
 * interoperate untouched — viewers never need the mod, only typists send anything.
 * C->S only; there is no S->C traffic at all.
 */
object CohPayloads {
    /** Wire cap. Other mods (chatsounds) raise the chat box limit far beyond vanilla's 256. */
    const val MAX_TEXT = 512

    const val KIND_START = 0
    const val KIND_TEXT = 1
    const val KIND_END = 2

    /** C->S: live typing state. Full string every time (MC chat is ≤256 chars, no diffing needed). */
    class TypingPayload(val kind: Int, val text: String) : CustomPacketPayload {
        companion object {
            val TYPE = CustomPacketPayload.Type<TypingPayload>(Identifier.fromNamespaceAndPath(Coh.MOD_ID, "typing"))
            val CODEC: StreamCodec<ByteBuf, TypingPayload> = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, TypingPayload::kind,
                ByteBufCodecs.stringUtf8(MAX_TEXT), TypingPayload::text,
                ::TypingPayload,
            )
        }

        override fun type() = TYPE
    }
}
