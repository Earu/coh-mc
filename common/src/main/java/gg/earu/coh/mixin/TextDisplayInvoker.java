package gg.earu.coh.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.TextDisplay.class)
public interface TextDisplayInvoker {
    @Invoker("setText")
    void coh$setText(Component text);

    @Invoker("setLineWidth")
    void coh$setLineWidth(int width);

    @Invoker("setBackgroundColor")
    void coh$setBackgroundColor(int argb);

    @Invoker("setTextOpacity")
    void coh$setTextOpacity(byte opacity);
}
