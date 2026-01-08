package passive.leveling.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// We target ItemStack because it handles the final speed calculation in 1.21
@Mixin(ItemStack.class)
public class MiningToolMixin {

    @Inject(method = "getMiningSpeedMultiplier", at = @At("HEAD"), cancellable = true)
    private void checkBroken(BlockState state, CallbackInfoReturnable<Float> cir) {
        // In a Mixin targeting ItemStack, 'this' is the stack instance.
        ItemStack stack = (ItemStack) (Object) this;

        // If the item has durability and is at (Max - 1) damage
        if (stack.isDamageable() && stack.getDamage() >= stack.getMaxDamage() - 1) {
            // Force mining speed to 1.0f (same as using your fist)
            cir.setReturnValue(1.0f);
        }
    }
}