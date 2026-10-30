package factoryage.compat.mixin;

import appeng.api.config.PowerMultiplier;
import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPartItem;
import appeng.parts.automation.UpgradeablePart;
import factoryage.compat.CompatConfig;
import factoryage.compat.FluxPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Factory Age: the ExtendedAE Plus Entity Accelerator pays for each speed-up from AE power only.
 * Pay from the FE stored in the network's Applied Flux cells first; if there is not enough FE for this tick,
 * fall through to the original AE power path. Pseudo: nothing happens if ExtendedAE Plus is replaced or updated.
 */
@Pseudo
@Mixin(targets = "com.extendedae_plus.ae.parts.EntitySpeedTickerPart", remap = false)
public abstract class EntitySpeedTickerFluxMixin extends UpgradeablePart {
    protected EntitySpeedTickerFluxMixin(IPartItem<?> item) {
        super(item);
    }

    @Shadow
    protected abstract void setNetworkEnergySufficient(boolean sufficient);

    @Inject(method = "extractPower(D)Z", at = @At("HEAD"), cancellable = true)
    private void factoryage$preferFlux(double amount, CallbackInfoReturnable<Boolean> cir) {
        if (amount <= 0 || !CompatConfig.entityTickerPreferFlux()) {
            return;
        }
        try {
            IManagedGridNode node = this.getMainNode();
            IGrid grid = node == null ? null : node.getGrid();
            if (grid == null) {
                return;
            }
            // extractPower() is called with config-scaled units; the real AE cost is amount * the config multiplier.
            double aeCost = PowerMultiplier.CONFIG.multiply(amount);
            if (FluxPower.tryPay(grid, IActionSource.ofMachine(this), aeCost)) {
                this.setNetworkEnergySufficient(true);
                cir.setReturnValue(true);
            }
        } catch (Throwable ignored) {
            // Applied Flux missing or its API changed: use the original AE power path.
        }
    }
}
