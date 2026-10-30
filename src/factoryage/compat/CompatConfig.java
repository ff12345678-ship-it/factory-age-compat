package factoryage.compat;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CompatConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SKIP_RECIPE_ADVANCEMENTS;
    public static final ModConfigSpec.BooleanValue ENTITY_TICKER_PREFER_FLUX;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        ENTITY_TICKER_PREFER_FLUX = builder
            .comment("ExtendedAE Plus Entity Accelerator: pay from the FE stored in the network's Applied Flux cells first,",
                     "and use AE power only when the stored FE is not enough for that tick.")
            .define("entityTickerPreferFlux", true);
        SKIP_RECIPE_ADVANCEMENTS = builder
            .comment("Skip loading the recipe-unlock advancements (ids with a path starting with recipes/).",
                     "Saves the per-inventory-change criterion checks; the vanilla recipe book no longer fills itself.",
                     "Set to false to restore them (needs a world reload).")
            .define("skipRecipeAdvancements", true);
        SPEC = builder.build();
    }

    private CompatConfig() {
    }

    public static boolean entityTickerPreferFlux() {
        try {
            return ENTITY_TICKER_PREFER_FLUX.get();
        } catch (IllegalStateException notLoaded) {
            return ENTITY_TICKER_PREFER_FLUX.getDefault();
        }
    }

    public static boolean skipRecipeAdvancements() {
        try {
            return SKIP_RECIPE_ADVANCEMENTS.get();
        } catch (IllegalStateException notLoaded) {
            return SKIP_RECIPE_ADVANCEMENTS.getDefault();
        }
    }
}
