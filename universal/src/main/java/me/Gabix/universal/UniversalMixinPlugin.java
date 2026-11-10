package me.Gabix.universal;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * One plugin instance is created for every mixin configuration in the universal
 * jar.  A config only applies on its own Minecraft version; all other copies are
 * inert.  This is what allows three independently compiled variants to share a
 * single jar without their mixins fighting each other.
 */
public final class UniversalMixinPlugin implements IMixinConfigPlugin {
    private static final boolean SODIUM_LOADED = FabricLoader.getInstance().isModLoaded("sodium");
    private static final boolean ALLOW_RENDER_LAYER_MIXINS;

    static {
        FabricLoader loader = FabricLoader.getInstance();
        if (loader.isModLoaded("canvas") || loader.isModLoaded("iris")) {
            ALLOW_RENDER_LAYER_MIXINS = false;
        } else {
            ALLOW_RENDER_LAYER_MIXINS = true;
        }
    }

    private boolean active;
    private boolean chunkConfig;
    private boolean satinConfig;

    @Override
    public void onLoad(String mixinPackage) {
        String prefix = VersionSelector.prefix();
        active = mixinPackage.startsWith(prefix + ".");
        chunkConfig = mixinPackage.contains(".chunk.mixin");
        satinConfig = mixinPackage.contains(".ladysnake.satin.mixin.");
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!active) {
            return false;
        }
        if (chunkConfig && SODIUM_LOADED) {
            return false;
        }
        if (satinConfig && mixinClassName.contains("blockrenderlayer")) {
            return ALLOW_RENDER_LAYER_MIXINS;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
