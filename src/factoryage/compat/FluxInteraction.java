package factoryage.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import sonar.fluxnetworks.register.EventHandler;

@EventBusSubscriber(modid = "factory_age_compat")
public final class FluxInteraction {
    private static final ResourceLocation BASE = ResourceLocation.parse("kubejs:compressed_obsidian");

    private FluxInteraction() {}

    @SubscribeEvent
    public static void leftClick(PlayerInteractEvent.LeftClickBlock event) {
        var level = event.getLevel();
        if (level.isClientSide() || event.isCanceled()
                || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }
        var pos = event.getPos();
        if (level.getBlockState(pos).getBlock() != Blocks.OBSIDIAN
                || !level.getBlockState(pos.below()).isAir()) {
            return;
        }
        var basePos = pos.below(2);
        var base = level.getBlockState(basePos);
        if (!BuiltInRegistries.BLOCK.getKey(base.getBlock()).equals(BASE)) {
            return;
        }
        // Synchronous substitution solely for Flux's hard-coded base check.
        // Flag 4 sends no neighbor/client updates. Always restore, even on error.
        if (!level.setBlock(basePos, Blocks.BEDROCK.defaultBlockState(), 4)) {
            return;
        }
        try {
            EventHandler.onPlayerInteract(event);
        } finally {
            level.setBlock(basePos, base, 4);
        }
    }
}
