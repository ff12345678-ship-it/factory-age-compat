package factoryage.compat;

import net.darkhax.pricklemc.common.api.config.property.IDefaultPropertyAdapters;
import net.darkhax.runelic.common.impl.RunelicMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import java.util.ServiceLoader;

@Mod("factory_age_compat")
public final class FactoryAgeCompat {
    public FactoryAgeCompat(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, CompatConfig.SPEC);
        // ProjectE logs "RECEIVED TRANSMUTATION EMC DATA" at debug for every EMC sync packet (several per tick
        // with EMC links running), which filled debug.log with over a million lines per session.
        Configurator.setLevel("moze_intel.projecte.PECore", Level.INFO);
        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Prickle uses the thread context loader for ServiceLoader. A vanilla
            // world-loading worker may see no Minecraft property providers.
            Thread thread = Thread.currentThread();
            ClassLoader previous = thread.getContextClassLoader();
            try {
                ClassLoader loader = RunelicMod.class.getClassLoader();
                thread.setContextClassLoader(loader);
                boolean hasMinecraftAdapter = ServiceLoader.load(IDefaultPropertyAdapters.class, loader)
                    .stream().anyMatch(p -> p.type().getName().equals(
                        "net.darkhax.pricklemc.common.impl.config.property.MinecraftPropertyPlugin"));
                if (!hasMinecraftAdapter) {
                    throw new IllegalStateException("Prickle Minecraft config adapter is unavailable");
                }
                RunelicMod.CONFIG.get();
                RunelicMod.LOG.info("Factory Age: Runelic configuration preloaded with Minecraft property adapters");
            } finally {
                thread.setContextClassLoader(previous);
            }
        });
    }
}
