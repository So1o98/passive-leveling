package passive.leveling.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import passive.leveling.PassiveLeveling;
import passive.leveling.TierUnlocker;

@Mixin(CraftingScreenHandler.class)
public class CraftingScreenHandlerMixin {

    @Inject(method = "updateResult", at = @At("RETURN"))
    private static void onUpdateResult(ScreenHandler handler, World world, PlayerEntity player, RecipeInputInventory input, CraftingResultInventory result, CallbackInfo ci) {
        if (PassiveLeveling.CONFIG == null || !PassiveLeveling.CONFIG.restrictions.lock_recipes_to_tier) return;

        ItemStack output = result.getStack(0);
        if (output.isEmpty()) return;

        if (!TierUnlocker.canCraft(player, output.getItem())) {
            result.setStack(0, ItemStack.EMPTY);
        }
    }
}