package gg.earu.coh.mixin;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The Display setters are private in Mojmap; entity data replication makes them safe to call server-side. */
@Mixin(Display.class)
public interface DisplayInvoker {
    @Invoker("setBillboardConstraints")
    void coh$setBillboardConstraints(Display.BillboardConstraints constraints);

    @Invoker("setViewRange")
    void coh$setViewRange(float range);

    @Invoker("setTransformation")
    void coh$setTransformation(Transformation transformation);
}
