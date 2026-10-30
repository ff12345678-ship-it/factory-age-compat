package factoryage.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.extensions.IEntityExtension;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

// Natural Charged Snowball (kubejs:natural_charged_snowball): thrown like Powah's charged snowball,
// but the bolt has no cause and carries AE2 Lightning Tech's natural-weather marker.
@EventBusSubscriber(modid = "factory_age_compat")
public final class NaturalSnowball {
    private static final ResourceLocation ITEM = ResourceLocation.parse("kubejs:natural_charged_snowball");
    private static final String FLAG = "factory_age.natural_charged_snowball";
    private static final String AE2LT_NATURAL = "ae2lt.natural_weather_lightning";

    private NaturalSnowball() {}

    // NeoForge adds getPersistentData to Entity at runtime; the compile classpath has the unpatched class.
    private static CompoundTag data(Entity entity) {
        return ((IEntityExtension) (Object) entity).getPersistentData();
    }

    @SubscribeEvent
    public static void use(PlayerInteractEvent.RightClickItem event) {
        var stack = event.getItemStack();
        if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ITEM)) {
            return;
        }
        var player = event.getEntity();
        var level = event.getLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW,
            SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide()) {
            var ball = new Snowball(level, player);
            ball.setItem(stack.copyWithCount(1));
            ball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            data(ball).putBoolean(FLAG, true);
            level.addFreshEntity(ball);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
    }

    @SubscribeEvent
    public static void impact(ProjectileImpactEvent event) {
        var projectile = event.getProjectile();
        if (!(projectile instanceof Snowball) || !(projectile.level() instanceof ServerLevel level)) {
            return;
        }
        var data = data(projectile);
        if (!data.getBoolean(FLAG)) {
            return;
        }
        data.remove(FLAG);
        var bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return;
        }
        bolt.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(event.getRayTraceResult().getLocation())));
        data(bolt).putBoolean(AE2LT_NATURAL, true);
        level.addFreshEntity(bolt);
    }
}
