package org.mcsr.speedrunapi.mixin_plugin;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.mcsr.speedrunapi.SpeedrunAPI;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * A MixinConfigPlugin that can be used by mods directly, or extended to build on added functionality.
 * <p>
 * Current features are:
 * <p>
 * - Automatically disables mixins in the "the.mods.mixinPackage.compat.modid" package if the targeted mod isn't present.
 */
public class SpeedrunMixinConfigPlugin implements IMixinConfigPlugin {
    protected String mixinPackage;

    @Override
    public void onLoad(String mixinPackage) {
        this.mixinPackage = mixinPackage;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // ensure we do not force apply any mixin when the target class isn't present, otherwise causing a crash
        if (SpeedrunMixinConfigPlugin.class.getClassLoader().getResource(targetClassName.replace('.', '/') + ".class") == null) {
            SpeedrunAPI.LOGGER.warn("target class {} for mixin {} not found", targetClassName, mixinClassName);
            return false;
        }
        String compatPackage = this.mixinPackage + ".compat.";
        if (mixinClassName.startsWith(compatPackage)) {
            String modid = mixinClassName.substring(compatPackage.length()).split("\\.", 2)[0];
            for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
                if (mod.getMetadata().getId().replace('-', '_').equals(modid)) {
                    return true;
                }
                for (String provided : mod.getMetadata().getProvides()) {
                    if (provided.replace('-', '_').equals(modid)) {
                        return true;
                    }
                }
            }
            return false;
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
