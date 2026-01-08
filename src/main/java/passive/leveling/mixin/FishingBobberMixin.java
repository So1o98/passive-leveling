package passive.leveling.mixin;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import passive.leveling.LevelingHandler;
import passive.leveling.PassiveLevelingConfig;

@Mixin(FishingBobberEntity.class)
public class FishingBobberMixin {

    @Inject(method = "use", at = @At("RETURN"))
    private void onUse(ItemStack usedItem, CallbackInfoReturnable<Integer> cir) {
        int damageTaken = cir.getReturnValue();

        if (damageTaken > 0) {
            FishingBobberEntity bobber = (FishingBobberEntity) (Object) this;

            if (bobber.getOwner() instanceof ServerPlayerEntity player) {
                if (usedItem.getDamage() < usedItem.getMaxDamage() - 1) {

                    PassiveLevelingConfig config = AutoConfig.getConfigHolder(PassiveLevelingConfig.class).getConfig();
                    int xp = config.fishingXp; // Updated

                    LevelingHandler.addXp(player, usedItem, xp);
                }
            }
        }
    }
}