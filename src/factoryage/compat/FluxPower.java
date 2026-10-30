package factoryage.compat;

import appeng.api.config.Actionable;
import appeng.api.config.PowerUnit;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.MEStorage;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;

/**
 * Pays an AE power cost from the FE stored in the network's Applied Flux cells.
 * Kept in its own class so Applied Flux is only touched when this is actually called.
 */
public final class FluxPower {
    private FluxPower() {
    }

    /** Takes the FE equivalent of {@code aeUnits} from the network storage; all or nothing. */
    public static boolean tryPay(IGrid grid, IActionSource source, double aeUnits) {
        long fe = (long) Math.ceil(PowerUnit.AE.convertTo(PowerUnit.FE, aeUnits));
        if (fe <= 0) {
            return true;
        }
        MEStorage storage = grid.getStorageService().getInventory();
        FluxKey key = FluxKey.of(EnergyType.FE);
        if (storage.extract(key, fe, Actionable.SIMULATE, source) < fe) {
            return false;
        }
        return storage.extract(key, fe, Actionable.MODULATE, source) >= fe;
    }
}
