package passive.leveling.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import passive.leveling.RecipeUnlockHandler;

@Mixin(CraftingScreenHandler.class)
public abstract class CraftingScreenHandlerMixin {

    @Inject(method = "updateResult", at = @At("TAIL"))
    private static void validateTierCrafting(ScreenHandler handler, World world, PlayerEntity player,
                                             RecipeInputInventory inventory,
                                             CraftingResultInventory resultInventory,
                                             @Nullable RecipeEntry<?> recipe,
                                             CallbackInfo ci) {

        if (player instanceof ServerPlayerEntity serverPlayer) {
            ItemStack resultStack = resultInventory.getStack(0);

            if (!resultStack.isEmpty()) {
                // If the output is locked (e.g. Stone Pickaxe but no Level 10 Wood Pickaxe)
                if (RecipeUnlockHandler.isRecipeLocked(serverPlayer, resultStack)) {
                    // Set the output to AIR (empty)
                    resultInventory.setStack(0, ItemStack.EMPTY);

                    // Force the server to tell the client "There is nothing here"
                    serverPlayer.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket(handler.syncId, handler.nextRevision(), 0, ItemStack.EMPTY));
                }
            }
        }
    }
}