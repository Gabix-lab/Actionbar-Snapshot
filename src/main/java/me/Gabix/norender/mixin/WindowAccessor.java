package me.Gabix.norender.mixin;

import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Window.class)
public interface WindowAccessor {
    @Accessor("framerateLimit")
    int getFramerateLimit();

    @Accessor("framerateLimit")
    void setFramerateLimit(int limit);
}