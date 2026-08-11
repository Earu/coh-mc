package gg.earu.coh.mixin;

import gg.earu.coh.server.DisplayManager;
import java.util.Set;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A COH display that reaches the disk outlives the message it belongs to: the chunk it was in
 * unloads before the popup expires (dimension change, death, disconnect, crash) and every later
 * load resurrects the same text, forever. Ours are never written out, so an unloading chunk
 * simply forgets them.
 */
@Mixin(Entity.class)
public abstract class EntitySaveMixin {
    @Shadow
    public abstract Set<String> getTags();

    @Inject(method = "shouldBeSaved", at = @At("HEAD"), cancellable = true)
    private void coh$dropDisplays(CallbackInfoReturnable<Boolean> cir) {
        if (this.getTags().contains(DisplayManager.ORPHAN_TAG)) {
            cir.setReturnValue(false);
        }
    }
}
