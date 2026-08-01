package gg.earu.coh.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * startRiding refuses non-serializable vehicles (players) on newer versions, so the
 * display mounts by hand: set the vehicle field, then addPassenger — the exact vanilla
 * mount sequence minus the checks. ServerEntity syncs passenger changes automatically.
 */
@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("vehicle")
    void coh$setVehicle(Entity vehicle);

    @Invoker("addPassenger")
    void coh$addPassenger(Entity passenger);
}
