package gg.earu.coh.core

import gg.earu.coh.Coh
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path

enum class FollowMode {
    /** Display rides the player as a passenger (default, smoothest). */
    RIDE,

    /** Display is teleported to the player every tick (fallback when other mods dismount passengers). */
    TELEPORT,
}

@Serializable
data class ServerConfig(
    val enabled: Boolean = true,
    /** text_display view range multiplier; 0.5 ≈ 32 blocks at default client entity distance. */
    val viewRange: Float = 0.5f,
    /** Minimum interval between accepted text updates per player; late updates queue, never drop. */
    val throttleMs: Int = 150,
    /** How long a sent chat message lingers above the head. */
    val popupSeconds: Double = 6.0,
    /** Typing sessions with no updates for this long are force-ended. */
    val idleTimeoutSeconds: Double = 15.0,
    val maxChars: Int = 256,
    val lineWidth: Int = 200,
    val hideWhileSneaking: Boolean = true,
    /** Show a "…" bubble while a player has chat open but hasn't typed anything visible. */
    val showTypingIndicator: Boolean = true,
    val followMode: FollowMode = FollowMode.RIDE,
    /** Vertical offset of the bubble relative to the ride/anchor position. */
    val headOffsetY: Double = 0.9,
)

@Serializable
data class ClientConfig(
    /** Master toggle: when off, nothing is ever transmitted while typing. */
    val enabled: Boolean = true,
)

object CohConfig {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun loadServer(configDir: Path): ServerConfig = load(configDir.resolve("server.json"), ServerConfig())

    fun loadClient(configDir: Path): ClientConfig = load(configDir.resolve("client.json"), ClientConfig())

    private inline fun <reified T> load(file: Path, defaults: T): T {
        try {
            if (Files.exists(file)) {
                return json.decodeFromString<T>(Files.readString(file))
            }
            Files.createDirectories(file.parent)
            Files.writeString(file, json.encodeToString(defaults))
        } catch (e: Exception) {
            Coh.LOGGER.error("Failed to load {}, using defaults", file, e)
        }
        return defaults
    }
}
