package passive.leveling.mixin;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Shadow public abstract int getMaxDamage();
    @Shadow public abstract int getDamage();
    @Shadow public abstract void setDamage(int damage);
    @Shadow public abstract boolean isDamageable();
    @Shadow public abstract Item getItem();

    @Inject(method = "damage(ILnet/minecraft/server/world/ServerWorld;Lnet/minecraft/server/network/ServerPlayerEntity;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"), cancellable = true)
    private void onDamage(int amount, ServerWorld world, ServerPlayerEntity player, Consumer<Item> breakCallback, CallbackInfo ci) {
        if (this.isDamageable()) {
            int currentDamage = this.getDamage();
            int maxDamage = this.getMaxDamage();

            // If this damage would break the item (or if it's already at the limit)
            if (currentDamage + amount >= maxDamage) {
                // 1. Prevent the damage logic from running (stops breakage)
                ci.cancel();

                // 2. Set durability to exactly 1 left (Damage = Max - 1)
                this.setDamage(maxDamage - 1);

                // 3. Warn the player (only if they aren't already spammed)
                // We assume if damage is increasing, they tried to use it.
                if (player != null) {
                    player.sendMessage(Text.literal("⚠ This item is broken please repair ⚠").formatted(Formatting.RED, Formatting.BOLD), true);

                    // Optional: Play a sound to indicate failure?
                    // player.playSound(net.minecraft.sound.SoundEvents.ITEM_SHIELD_BREAK, 1.0f, 1.0f);
                }
            }
        }
    }
}