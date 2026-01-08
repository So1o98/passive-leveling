package passive.leveling;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;

public class CombatHandler implements ServerLivingEntityEvents.AfterDeath, ServerLivingEntityEvents.AllowDamage {

    @Override
    public boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (source.getSource() instanceof PlayerEntity) {
            if (source.getSource() instanceof ArrowEntity || source.getSource() instanceof PersistentProjectileEntity) {
                PlayerEntity player = (PlayerEntity) source.getSource();
                ItemStack stack = player.getMainHandStack();
                // UPDATED: projectile_xp
                int xpAmount = PassiveLeveling.CONFIG.rewards.projectile_xp;
                new LevelingHandler().addXp(player.getWorld(), player, stack, xpAmount);
            }
        }
        return true;
    }

    @Override
    public void afterDeath(LivingEntity entity, DamageSource damageSource) {
        if (!(damageSource.getAttacker() instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) damageSource.getAttacker();
        ItemStack stack = player.getMainHandStack();

        int xpToGive = getMobXp(entity);
        if (xpToGive > 0) {
            new LevelingHandler().addXp(player.getWorld(), player, stack, xpToGive);
        }
    }

    private int getMobXp(LivingEntity entity) {
        if (PassiveLeveling.CONFIG == null) return 0;
        ModConfig.Rewards config = PassiveLeveling.CONFIG.rewards;

        if (!entity.canUsePortals()) return config.boss_mob_xp;
        if (entity instanceof Monster || entity instanceof HostileEntity) return config.hostile_mob_xp;
        if (entity instanceof PassiveEntity) return config.passive_mob_xp;

        return 0;
    }
}