package factoryage.compat;

import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import sonar.fluxnetworks.client.jei.CreatingFluxRecipe;
import sonar.fluxnetworks.client.jei.CreatingFluxRecipeCategory;

@JeiPlugin
public final class FluxJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("factory_age_compat", "flux_dust");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var base = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("kubejs:compressed_obsidian"));
        var output = BuiltInRegistries.ITEM.get(ResourceLocation.parse("fluxnetworks:flux_dust"));
        if (base == Blocks.AIR || output == Items.AIR) {
            throw new IllegalStateException("Factory Age Flux Dust ingredients are not registered");
        }
        registration.addRecipes(CreatingFluxRecipeCategory.RECIPE_TYPE, List.of(
            new CreatingFluxRecipe(base, Blocks.OBSIDIAN,
                new ItemStack(Items.REDSTONE), new ItemStack(output))
        ));
    }
}
