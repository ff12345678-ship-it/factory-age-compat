package factoryage.compat.mixin;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.ILinkStatus;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.MEStorage;
import appeng.core.definitions.AEItems;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.slot.RestrictedInputSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Factory Age: when the Pattern Encoding Terminal needs a blank pattern and its own blank slot is empty,
 * take one from the ME network, so patterns can be encoded without stocking the terminal slot by hand.
 * Replaces the network-backed blank slot of Data Energistics (removed in 0.7.7) with a server-side pull in encode().
 */
@Mixin(value = PatternEncodingTermMenu.class, remap = false)
public abstract class PatternEncodingTermMenuMixin extends MEStorageMenu {
    @Shadow @Final private RestrictedInputSlot blankPatternSlot;
    @Shadow @Final private RestrictedInputSlot encodedPatternSlot;

    @Unique private boolean factoryage$pulledBlank;

    protected PatternEncodingTermMenuMixin(MenuType<?> type, int id, Inventory inv, ITerminalHost host) {
        super(type, id, inv, host);
    }

    @Inject(method = "encode", at = @At("HEAD"))
    private void factoryage$pullBlankPattern(CallbackInfo ci) {
        this.factoryage$pulledBlank = false;
        if (this.isClientSide()) {
            return;
        }
        // AE2 only consumes a blank pattern when the encoded-pattern slot is empty; otherwise it overwrites that item.
        if (!this.encodedPatternSlot.getItem().isEmpty() || !this.blankPatternSlot.getItem().isEmpty()) {
            return;
        }
        MEStorage network = this.storage;
        ILinkStatus link = this.getLinkStatus();
        if (network == null || (link != null && !link.connected())) {
            return;
        }
        long taken = network.extract(AEItemKey.of(AEItems.BLANK_PATTERN), 1, Actionable.MODULATE, this.getActionSource());
        if (taken > 0) {
            this.blankPatternSlot.set(AEItems.BLANK_PATTERN.stack());
            this.factoryage$pulledBlank = true;
        }
    }

    @Inject(method = "encode", at = @At("RETURN"))
    private void factoryage$returnUnusedBlankPattern(CallbackInfo ci) {
        if (!this.factoryage$pulledBlank) {
            return;
        }
        this.factoryage$pulledBlank = false;
        // Encoding did not happen (incomplete recipe, etc.): the pulled blank pattern is still in the slot, give it back.
        ItemStack left = this.blankPatternSlot.getItem();
        if (left.isEmpty() || !AEItems.BLANK_PATTERN.is(left)) {
            return;
        }
        long count = left.getCount();
        long inserted = this.storage.insert(AEItemKey.of(AEItems.BLANK_PATTERN), count, Actionable.MODULATE, this.getActionSource());
        if (inserted >= count) {
            this.blankPatternSlot.set(ItemStack.EMPTY);
        } else if (inserted > 0) {
            this.blankPatternSlot.set(AEItems.BLANK_PATTERN.stack((int) (count - inserted)));
        }
    }
}
