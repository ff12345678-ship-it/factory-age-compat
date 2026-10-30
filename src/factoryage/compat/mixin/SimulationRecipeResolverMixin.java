package factoryage.compat.mixin;

import java.util.List;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Factory Age: the Mekanical Create factory matches recipes by unordered ingredient multiset, so vanilla
 * crafting-table recipes with identical ingredients (stairs vs walls, pickaxe vs axe, ...) cannot be told apart.
 * Skip the vanilla "crafting" process entirely; Create's own mechanical crafting recipes stay available.
 */
@Mixin(targets = "io.github.langqi99.mekanicalcreate.content.SimulationRecipeResolver", remap = false)
public abstract class SimulationRecipeResolverMixin {
    @Inject(method = "addCrafting", at = @At("HEAD"), cancellable = true)
    private static void factoryage$skipWorkbenchRecipes(List<?> candidates, List<?> recipes, String process,
                                                         int priority, Level level, CallbackInfo ci) {
        if ("crafting".equals(process)) {
            ci.cancel();
        }
    }
}
