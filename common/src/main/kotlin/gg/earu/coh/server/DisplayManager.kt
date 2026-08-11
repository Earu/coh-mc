package gg.earu.coh.server

import com.google.common.collect.ImmutableList
import com.mojang.math.Transformation
import gg.earu.coh.Coh
import gg.earu.coh.core.DisplaySink
import gg.earu.coh.core.FollowMode
import gg.earu.coh.core.ServerConfig
import gg.earu.coh.mixin.DisplayInvoker
import gg.earu.coh.mixin.EntityAccessor
import gg.earu.coh.mixin.TextDisplayInvoker
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket
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

        /** Cleared when the message is sent: the popup then stays where the player sent it. */
        var following = true

        /** Ticks left to (re)send the mount to the ridden player's own client, see [DisplayManager.syncMountToRider]. */
        var mountSyncTicks = MOUNT_SYNC_TICKS
    }

    private var server: MinecraftServer? = null
    private val tracked = HashMap<UUID, Tracked>()
    private var ticksSinceSweep = 0

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
        val player = server?.playerList?.getPlayer(playerId)
        if (player == null) {
            Coh.LOGGER.debug("spawn: no server attached or player {} offline (server={})", playerId, server != null)
            return
        }
        val entity = createFor(player, text)
        if (entity == null) {
            Coh.LOGGER.debug("spawn: createFor failed for {}", player.scoreboardName)
            return
        }
        Coh.LOGGER.debug("spawn: display {} riding {}", entity.id, player.scoreboardName)
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

    /**
     * The dismount itself waits for the next tick: a display spawned this very tick (vanilla
     * typist, no typing session) has not been anchored by the ride yet, and freezing it early
     * would leave it at the rough spawn height instead of where the bubble actually renders.
     */
    override fun freeze(playerId: UUID) {
        tracked[playerId]?.following = false
    }

    override fun remove(playerId: UUID) {
        tracked.remove(playerId)?.entity?.discard()
    }

    fun removeAll() {
        tracked.values.forEach { it.entity.discard() }
        tracked.clear()
    }

    /**
     * Per-tick watchdog: detaches sent popups, repairs dismounted/stranded displays (covers death,
     * dimension change, other mods), syncs the mount to the rider, sneak hiding, teleport-follow,
     * and sweeps leftovers on a timer.
     */
    fun tick() {
        val server = server ?: return
        if (++ticksSinceSweep >= SWEEP_INTERVAL_TICKS) sweepOrphans(server)
        if (tracked.isEmpty()) return
        val iterator = tracked.entries.iterator()
        while (iterator.hasNext()) {
            val (id, t) = iterator.next()
            val player = server.playerList.getPlayer(id)
            if (player == null) {
                t.entity.discard()
                iterator.remove()
                continue
            }

            if (!t.following) {
                detach(t, player)
                // A sent popup keeps its spot, but only in the world its sender is still in: one
                // stranded by a dimension change or a respawn elsewhere is dropped right away
                // instead of hanging around a world nobody can tie it back to.
                if (t.entity.isRemoved || t.entity.level() !== player.level()) {
                    t.entity.discard()
                    iterator.remove()
                }
                continue
            }

            val broken = t.entity.isRemoved ||
                t.entity.level() !== player.level() ||
                (config.followMode == FollowMode.RIDE && t.entity.vehicle !== player)
            if (broken) {
                t.entity.discard()
                t.entity = createFor(player, t.text) ?: continue // retry next tick
                t.hidden = false
                t.mountSyncTicks = MOUNT_SYNC_TICKS
            }

            if (t.mountSyncTicks > 0) {
                t.mountSyncTicks--
                if (t.entity.vehicle === player) syncMountToRider(player)
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

    /**
     * A display is never meant to outlive its message, so any tagged one we do not own is a
     * leftover: worlds saved before displays stopped being written to disk still hold them, and a
     * crash or another mod's copy can strand one at any time. Runs on startup and then on a timer,
     * since a leftover only becomes reachable once its chunk loads.
     */
    fun sweepOrphans(server: MinecraftServer) {
        ticksSinceSweep = 0
        val live = tracked.values.mapTo(HashSet(tracked.size)) { it.entity.id }
        var count = 0
        for (level in server.allLevels) {
            val orphans = level.getEntities(EntityType.TEXT_DISPLAY) {
                it.tags.contains(ORPHAN_TAG) && it.id !in live
            }
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

        if (!level.addFreshEntity(entity)) {
            Coh.LOGGER.debug("createFor: addFreshEntity refused")
            return null
        }
        if (config.followMode == FollowMode.RIDE && !forceMount(entity, player)) {
            Coh.LOGGER.debug("createFor: mount failed")
            entity.discard()
            return null
        }
        return entity
    }

    /** Sent message: cut the display loose so it lingers where the player stood, fully visible. */
    private fun detach(t: Tracked, player: ServerPlayer) {
        t.mountSyncTicks = 0
        if (t.hidden) {
            t.hidden = false
            (t.entity as DisplayInvoker).`coh$setViewRange`(config.viewRange)
        }
        if (t.entity.vehicle !== player) return
        forceDismount(t.entity, player)
        syncMountToRider(player)
    }

    /**
     * Vanilla startRiding refuses players as vehicles (EntityType.PLAYER doesn't serialize),
     * so mount manually: the exact vanilla sequence minus its checks. No game event is fired
     * (no sculk triggers) and the passenger packet syncs automatically via ServerEntity.
     */
    private fun forceMount(passenger: Display.TextDisplay, vehicle: ServerPlayer): Boolean {
        (passenger as EntityAccessor).`coh$setVehicle`(vehicle)
        (vehicle as EntityAccessor).`coh$addPassenger`(passenger)
        return passenger.vehicle === vehicle
    }

    /** Mirror of [forceMount]; writes the list directly so no ENTITY_DISMOUNT game event fires. */
    private fun forceDismount(passenger: Display.TextDisplay, vehicle: ServerPlayer) {
        (passenger as EntityAccessor).`coh$setVehicle`(null)
        (vehicle as EntityAccessor).`coh$setPassengers`(
            ImmutableList.copyOf(vehicle.passengers.filter { it !== passenger })
        )
    }

    /**
     * A player is never inside their own entity tracker, so vanilla never tells them what is
     * riding them: without this the typist's own client keeps the bubble wherever it spawned
     * while it correctly follows everyone else's view. Cheap enough to resend on every mount.
     */
    private fun syncMountToRider(player: ServerPlayer) {
        player.connection.send(ClientboundSetPassengersPacket(player))
    }

    private fun renderText(text: String): Component =
        if (text.isEmpty()) TYPING_INDICATOR else Component.literal(text)

    companion object {
        const val ORPHAN_TAG = "coh_display"

        /** Extra rise above the ride anchor point; per-version tuning knob. */
        const val HEAD_OFFSET_BASE = 0f

        /**
         * Mount packets are sent from the tick loop, which may run before the display's own spawn
         * packet: a client that doesn't know the entity yet drops the mount, so send it twice.
         */
        private const val MOUNT_SYNC_TICKS = 2

        /** Orphan sweep cadence; a leftover is only visible for this long at worst. */
        private const val SWEEP_INTERVAL_TICKS = 100

        private const val BACKGROUND_ALPHA = 0xC8
        private const val BACKGROUND_RGB = 0x1E1E1E
        /** GMod panel color: dark grey, mostly opaque. */
        const val BACKGROUND = (BACKGROUND_ALPHA shl 24) or BACKGROUND_RGB

        private val TYPING_INDICATOR: Component = Component.literal("…").withStyle(ChatFormatting.GRAY)
    }
}
