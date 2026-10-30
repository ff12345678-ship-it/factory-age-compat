package factoryage.compat.mixin;

import com.google.gson.JsonElement;
import factoryage.compat.CompatConfig;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Factory Age: drop data files before they are parsed.
 * - Recipe-unlock advancements (path recipes/...): thousands of inventory_changed listeners, unused with JEI.
 * - Farming for Blockheads market recipes: KubeJS removes every market recipe anyway, and the bundled ones
 *   for absent mods each log a parse error on every reload.
 * Filtering in prepare() keeps clear of KubeJS's own hooks in RecipeManager.apply.
 */
@Mixin(value = SimpleJsonResourceReloadListener.class, remap = false)
public abstract class SimpleJsonResourceReloadListenerMixin {
    @Inject(method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Ljava/util/Map;",
            at = @At("RETURN"))
    private void factoryage$filter(ResourceManager manager, ProfilerFiller profiler,
                                   CallbackInfoReturnable<Map<ResourceLocation, JsonElement>> cir) {
        Object self = this;
        Map<ResourceLocation, JsonElement> map = cir.getReturnValue();
        if (self instanceof ServerAdvancementManager) {
            if (CompatConfig.skipRecipeAdvancements()) {
                map.keySet().removeIf(id -> id.getPath().startsWith("recipes/"));
            }
        } else if (self instanceof RecipeManager) {
            map.keySet().removeIf(id -> id.getNamespace().equals("farmingforblockheads") && id.getPath().startsWith("market/"));
        }
    }
}
