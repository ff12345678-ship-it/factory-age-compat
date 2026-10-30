package factoryage.compat;

import com.moakiee.ae2lt.worldgen.FirmamentStarshipPiece;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

// Firmament Starship Beacon (kubejs:firmament_starship_beacon): right-click (a block or the air) to build AE2 Lightning
// Tech's firmament starship with its Firmament Conversion Core one chunk in front of the player and the deck at the
// player's feet. Unlike /place structure, the structure start and references are written into the chunks, so the core
// (StructureManager.getStructureWithPieceAt) sees it as a real starship.
@EventBusSubscriber(modid = "factory_age_compat")
public final class StarshipBeacon {
    private static final ResourceLocation ITEM = ResourceLocation.parse("kubejs:firmament_starship_beacon");
    private static final ResourceLocation STARSHIP = ResourceLocation.parse("ae2lt:firmament_starship");
    private static final ResourceLocation TEMPLATE = ResourceLocation.parse("ae2lt:firmament_starship/firmament_starship");
    // template (80 x 20 x 80): conversion core at (35, 10, 42), the middle of the hull; floor at y 7, so feet go at y 8
    private static final int CORE_X = 35, CORE_Z = 42, FEET_Y = 8;
    private static final BlockPos PIVOT = new BlockPos(40, 0, 40);

    private StarshipBeacon() {}

    @SubscribeEvent
    public static void useOnBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isBeacon(event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
            build(event.getLevel(), event.getEntity(), event.getItemStack());
        }
    }

    @SubscribeEvent
    public static void useInAir(PlayerInteractEvent.RightClickItem event) {
        if (isBeacon(event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
            build(event.getLevel(), event.getEntity(), event.getItemStack());
        }
    }

    private static boolean isBeacon(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ITEM);
    }

    private static void build(Level anyLevel, Player player, ItemStack stack) {
        if (!(anyLevel instanceof ServerLevel level)) {
            return;
        }
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(STARSHIP);
        if (structure == null) {
            player.displayClientMessage(Component.translatable("factory_age.starship_beacon.missing"), true);
            return;
        }
        var facing = player.getDirection();
        var feet = player.blockPosition();
        var core = feet.relative(facing, 16);
        int originY = feet.getY() - FEET_Y;
        if (originY < level.getMinBuildHeight() || originY + 20 > level.getMaxBuildHeight()) {
            player.displayClientMessage(Component.translatable("factory_age.starship_beacon.height"), true);
            return;
        }
        var origin = new BlockPos(core.getX() - CORE_X, originY, core.getZ() - CORE_Z);
        var piece = new FirmamentStarshipPiece(level.getStructureManager(), TEMPLATE, origin, Rotation.NONE, PIVOT);
        var startChunk = new ChunkPos(core);
        var start = new StructureStart(structure, startChunk, 0, new PiecesContainer(List.of(piece)));
        BoundingBox box = start.getBoundingBox();
        int minX = SectionPos.blockToSectionCoord(box.minX()), maxX = SectionPos.blockToSectionCoord(box.maxX());
        int minZ = SectionPos.blockToSectionCoord(box.minZ()), maxZ = SectionPos.blockToSectionCoord(box.maxZ());
        // one starship per area: refuse when any covered chunk already belongs to one
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                var chunk = level.getChunk(x, z);
                if (chunk.getStartForStructure(structure) != null || !chunk.getReferencesForStructure(structure).isEmpty()) {
                    player.displayClientMessage(Component.translatable("factory_age.starship_beacon.occupied"), true);
                    return;
                }
            }
        }
        // register first: the core caches its "inside starship" check the first time it is asked
        var originChunk = level.getChunk(startChunk.x, startChunk.z);
        originChunk.setStartForStructure(structure, start);
        originChunk.setUnsaved(true);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                var chunk = level.getChunk(x, z);
                chunk.addReferenceForStructure(structure, startChunk.toLong());
                chunk.setUnsaved(true);
            }
        }
        var generator = level.getChunkSource().getGenerator();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                var cp = new ChunkPos(x, z);
                start.placeInChunk(level, level.structureManager(), generator, level.getRandom(),
                    new BoundingBox(cp.getMinBlockX(), level.getMinBuildHeight(), cp.getMinBlockZ(),
                        cp.getMaxBlockX(), level.getMaxBuildHeight() - 1, cp.getMaxBlockZ()), cp);
            }
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        int coreY = originY + 10;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("factory_age.starship_beacon.placed", core.getX(), coreY, core.getZ()), false);
    }
}
