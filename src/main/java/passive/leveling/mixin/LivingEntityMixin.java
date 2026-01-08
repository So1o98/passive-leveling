package passive.leveling.mixin;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import passive.leveling.LevelingHandler;
import passive.leveling.PassiveLevelingConfig;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "damage", at = @At("RETURN"))
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || amount <= 0) return;

        Entity attacker = source.getAttacker();
        if (!(attacker instanceof ServerPlayerEntity player)) return;
        if (player.isCreative()) return;

        ItemStack stack = player.getMainHandStack();
        if (stack.getItem() instanceof BowItem ||
                stack.getItem() instanceof CrossbowItem ||
                stack.getItem() instanceof TridentItem) {

            PassiveLevelingConfig config = AutoConfig.getConfigHolder(PassiveLevelingConfig.class).getConfig();
            int xp = config.projectileXp; // Updated

            LevelingHandler.addXp(player, stack, xp);
        }
    }
}