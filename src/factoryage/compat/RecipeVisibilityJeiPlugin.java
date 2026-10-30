package factoryage.compat;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** View-only adapters: never insert or replace recipes in the game's manager. */
@JeiPlugin
public final class RecipeVisibilityJeiPlugin implements IModPlugin {
    private static final Logger LOG = LoggerFactory.getLogger("FactoryAgeRecipeVisibility");
    private static final ResourceLocation MIXED = ResourceLocation.parse("quark:mixed_exclusion");
    private static final ResourceLocation LOG_CHEST = ResourceLocation.parse(
        "quark:building/crafting/chests/mixed_chest_wood_but_without_exclusions");

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.parse("factory_age_compat:recipe_visibility");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var views = new ArrayList<RecipeHolder<CraftingRecipe>>();
        for (var holder : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            var recipe = holder.value();
            var serializerId = BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer());
            if (ResourceLocation.parse("laserio:cardclear").equals(serializerId)) {
                // These four recipes have fixed shapeless inputs and intentionally
                // discard card settings. Keep the game's custom recipe untouched.
                var ingredients = net.minecraft.core.NonNullList.<Ingredient>create();
                ingredients.addAll(recipe.getIngredients());
                CraftingRecipe view = new ShapelessRecipe(recipe.getGroup(), recipe.category(),
                    recipe.getResultItem(level.registryAccess()).copy(), ingredients);
                views.add(new RecipeHolder<>(holder.id(), view));
                continue;
            }
            if (!MIXED.equals(serializerId)) continue;
            // Only this variant permits every combination of logs. The other
            // variants exclude matching wood sets and need different layouts.
            if (!holder.id().equals(LOG_CHEST)) continue;
            try {
                var serializer = (RecipeSerializer<CraftingRecipe>) recipe.getSerializer();
                JsonObject json = serializer.codec().codec().encodeStart(JsonOps.INSTANCE, recipe)
                    .getOrThrow().getAsJsonObject();
                var ingredient = Ingredient.of(TagKey.create(Registries.ITEM,
                    ResourceLocation.parse(json.get("tag").getAsString())));
                var pattern = ShapedRecipePattern.of(Map.of('#', ingredient), "###", "# #", "###");
                CraftingRecipe view = new ShapedRecipe(recipe.getGroup(), recipe.category(), pattern,
                    recipe.getResultItem(level.registryAccess()).copy());
                views.add(new RecipeHolder<>(holder.id(), view));
            } catch (RuntimeException e) {
                LOG.error("Cannot build JEI view for {}", holder.id(), e);
            }
        }
        registration.addRecipes(RecipeTypes.CRAFTING, views);
        LOG.info("Registered {} view-only Quark/LaserIO crafting recipes", views.size());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var manager = runtime.getRecipeManager();
        var visibleIds = new HashSet<ResourceLocation>();
        var registeredIds = new HashSet<ResourceLocation>();
        Set<Object> visibleObjects = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Object> registeredObjects = Collections.newSetFromMap(new IdentityHashMap<>());
        var categories = manager.createRecipeCategoryLookup().includeHidden().get().toList();
        for (var category : categories) {
            collect(manager, category.getRecipeType(), false, visibleIds, visibleObjects);
            collect(manager, category.getRecipeType(), true, registeredIds, registeredObjects);
        }
        var lines = new ArrayList<String>();
        lines.add("recipe_id\tgame_type\tserializer\tclass\tspecial\tresult\tstatus");
        int missingCrafting = 0;
        for (var holder : level.getRecipeManager().getRecipes()) {
            var recipe = holder.value();
            String status;
            if (visibleIds.contains(holder.id()) || visibleObjects.contains(recipe)) status = "registered_visible";
            else if (registeredIds.contains(holder.id()) || registeredObjects.contains(recipe)) status = "registered_hidden";
            else if (recipe.getType() == RecipeType.CRAFTING) {
                status = recipe.isSpecial() ? "special_crafting_needs_review" : "crafting_not_registered";
                missingCrafting++;
            } else status = "not_matched_may_use_generated_view";
            String output;
            try {
                var stack = recipe.getResultItem(level.registryAccess());
                output = stack.isEmpty() ? "dynamic_or_empty" : BuiltInRegistries.ITEM.getKey(stack.getItem()) + " x" + stack.getCount();
            } catch (RuntimeException e) { output = "result_error:" + e.getClass().getSimpleName(); }
            lines.add(holder.id() + "\t" + BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType())
                + "\t" + BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer())
                + "\t" + recipe.getClass().getName() + "\t" + recipe.isSpecial() + "\t" + output + "\t" + status);
        }
        try {
            var target = Minecraft.getInstance().gameDirectory.toPath().resolve("logs/factory-age-jei-audit.tsv");
            Files.createDirectories(target.getParent());
            Files.write(target, lines, StandardCharsets.UTF_8);
            LOG.info("JEI audit: {} game recipes, {} categories, {} unmatched crafting recipes; report {}",
                lines.size() - 1, categories.size(), missingCrafting, target);
        } catch (java.io.IOException e) { LOG.error("Could not save JEI recipe audit", e); }
    }

    private static <T> void collect(IRecipeManager manager, mezz.jei.api.recipe.RecipeType<T> type,
            boolean hidden, Set<ResourceLocation> ids, Set<Object> objects) {
        var lookup = manager.createRecipeLookup(type);
        if (hidden) lookup.includeHidden();
        lookup.get().forEach(value -> {
            if (value instanceof RecipeHolder<?> holder) {
                ids.add(holder.id()); objects.add(holder.value());
            } else objects.add(value);
        });
    }
}
