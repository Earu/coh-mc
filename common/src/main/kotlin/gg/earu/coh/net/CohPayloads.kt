package gg.earu.coh.net

/**
 * Wire message shared by every loader — plain data on this branch: 1.20.1 predates
 * CustomPacketPayload/StreamCodec, so each loader module owns its own serialization
 * (SimpleChannel on Forge, PacketByteBuf channels on Fabric). The channel is OPTIONAL:
 * vanilla clients/servers interoperate untouched — viewers never need the mod, only
 * typists send anything. C->S only; there is no S->C traffic at all.
 */
object CohPayloads {
    const val TYPING_PATH = "typing"

    /** Wire cap. Other mods (chatsounds) raise the chat box limit far beyond vanilla's 256. */
    const val MAX_TEXT = 512

    const val KIND_START = 0
    const val KIND_TEXT = 1
    const val KIND_END = 2

    /** C->S: live typing state. Full string every time, clamped to MAX_TEXT client-side. */
    class TypingPayload(val kind: Int, val text: String)
}
