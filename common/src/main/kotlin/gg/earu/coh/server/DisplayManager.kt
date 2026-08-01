package gg.earu.coh.server

import com.mojang.math.Transformation
import gg.earu.coh.Coh
import gg.earu.coh.core.DisplaySink
import gg.earu.coh.core.FollowMode
import gg.earu.coh.core.ServerConfig
import gg.earu.coh.mixin.DisplayInvoker
import gg.earu.coh.mixin.TextDisplayInvoker
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EntityType
import org.joml.Vector3f
import java.util.UUID

/**
 * Owns the text_display entities floating above typing players. All rendering is vanilla:
 * viewers need no mod — synched entity data does the whole job.
 */
class DisplayManager(private val configProvider: () -> ServerConfig) : DisplaySink {
    private class Tracked(var entity: Display.TextDisplay, var text: String) {
        var hidden = false
    }

    private var server: MinecraftServer? = null
    private val tracked = HashMap<UUID, Tracked>()

    private val config get() = configProvider()

    fun attach(server: MinecraftServer) {
        this.server = server
    }

    fun detach() {
        removeAll()
        server = null
    }

    override fun spawn(playerId: UUID, text: String) {
        remove(playerId)
        val player = server?.playerList?.getPlayer(playerId) ?: return
        val entity = createFor(player, text) ?: return
        tracked[playerId] = Tracked(entity, text)
    }

    override fun update(playerId: UUID, text: String) {
        val t = tracked[playerId] ?: return spawn(playerId, text)
        t.text = text
        val invoker = t.entity as TextDisplayInvoker
        invoker.`coh$setText`(renderText(text))
        invoker.`coh$setTextOpacity`(-1)
        invoker.`coh$setBackgroundColor`(BACKGROUND)
    }

    override fun fade(playerId: UUID, progress: Float) {
        val t = tracked[playerId] ?: return
        val f = (1f - progress).coerceIn(0f, 1f)
        val invoker = t.entity as TextDisplayInvoker
        // text_display quirk: text alpha below ~26 renders fully opaque, clamp there.
        invoker.`coh$setTextOpacity`((255 * f).toInt().coerceAtLeast(26).toByte())
        invoker.`coh$setBackgroundColor`(((BACKGROUND_ALPHA * f).toInt() shl 24) or BACKGROUND_RGB)
    }

    override fun remove(playerId: UUID) {
        tracked.remove(playerId)?.entity?.discard()
    }

    fun removeAll() {
        tracked.values.forEach { it.entity.discard() }
        tracked.clear()
    }

    /** Per-tick watchdog: repairs dismounted/stranded displays (covers death, dimension change, other mods), sneak hiding, teleport-follow. */
    fun tick() {
        if (tracked.isEmpty()) return
        val server = server ?: return
        val iterator = tracked.entries.iterator()
        while (iterator.hasNext()) {
            val (id, t) = iterator.next()
            val player = server.playerList.getPlayer(id)
            if (player == null) {
                t.entity.discard()
                iterator.remove()
                continue
            }

            val broken = t.entity.isRemoved ||
                t.entity.level() !== player.level() ||
                (config.followMode == FollowMode.RIDE && t.entity.vehicle !== player)
            if (broken) {
                t.entity.discard()
                t.entity = createFor(player, t.text) ?: continue // retry next tick
                t.hidden = false
            }

            if (config.followMode == FollowMode.TELEPORT) {
                t.entity.setPos(player.x, player.eyeY + 0.3, player.z)
            }

            if (config.hideWhileSneaking) {
                val hide = player.isCrouching
                if (hide != t.hidden) {
                    t.hidden = hide
                    (t.entity as DisplayInvoker).`coh$setViewRange`(if (hide) 0f else config.viewRange)
                }
            }
        }
    }

    /** Displays never legitimately persist; kill leftovers from crashes on startup. */
    fun sweepOrphans(server: MinecraftServer) {
        var count = 0
        for (level in server.allLevels) {
            val orphans = level.getEntities(EntityType.TEXT_DISPLAY) { it.tags.contains(ORPHAN_TAG) }
            orphans.forEach { it.discard() }
            count += orphans.size
        }
        if (count > 0) Coh.LOGGER.info("Swept {} orphaned COH display(s)", count)
    }

    private fun createFor(player: ServerPlayer, text: String): Display.TextDisplay? {
        val level = player.level() as? ServerLevel ?: return null
        val entity = EntityType.TEXT_DISPLAY.create(level) ?: return null
        entity.addTag(ORPHAN_TAG)
        entity.setPos(player.x, player.eyeY + 0.3, player.z)

        val display = entity as DisplayInvoker
        display.`coh$setBillboardConstraints`(Display.BillboardConstraints.CENTER)
        display.`coh$setViewRange`(config.viewRange)
        display.`coh$setTransformation`(
            Transformation(Vector3f(0f, HEAD_OFFSET_BASE + config.headOffsetY.toFloat(), 0f), null, null, null)
        )

        val textDisplay = entity as TextDisplayInvoker
        textDisplay.`coh$setLineWidth`(config.lineWidth)
        textDisplay.`coh$setBackgroundColor`(BACKGROUND)
        textDisplay.`coh$setText`(renderText(text))

        if (!level.addFreshEntity(entity)) return null
        if (config.followMode == FollowMode.RIDE && !entity.startRiding(player, true)) {
            // Riding refused (another mod interfering) — bail rather than churn; TELEPORT mode is the escape hatch.
            entity.discard()
            return null
        }
        return entity
    }

    private fun renderText(text: String): Component =
        if (text.isEmpty()) TYPING_INDICATOR else Component.literal(text)

    companion object {
        const val ORPHAN_TAG = "coh_display"

        /** Extra rise above the ride anchor point; per-version tuning knob. */
        const val HEAD_OFFSET_BASE = 0f

        private const val BACKGROUND_ALPHA = 0xC8
        private const val BACKGROUND_RGB = 0x1E1E1E
        /** GMod panel color: dark grey, mostly opaque. */
        const val BACKGROUND = (BACKGROUND_ALPHA shl 24) or BACKGROUND_RGB

        private val TYPING_INDICATOR: Component = Component.literal("…").withStyle(ChatFormatting.GRAY)
    }
}
