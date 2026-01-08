package passive.leveling.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import passive.leveling.PassiveLeveling;
import passive.leveling.SoulboundStorage;

import java.util.ArrayList;
import java.util.List;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    @Shadow @Final private PlayerInventory inventory;

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "dropInventory", at = @At("HEAD"))
    private void dropInventory(CallbackInfo ci) {
        // If KeepInventory gamerule is ON, do nothing
        if (this.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) return;

        List<ItemStack> soulboundItems = new ArrayList<>();
        int reqLevel = PassiveLeveling.CONFIG.leveling.soulbound_level;

        // Scan all inventory slots
        scanAndSave(this.inventory.main, soulboundItems, reqLevel);
        scanAndSave(this.inventory.armor, soulboundItems, reqLevel);
        scanAndSave(this.inventory.offHand, soulboundItems, reqLevel);

        // Save found items to the vault
        if (!soulboundItems.isEmpty()) {
            SoulboundStorage.SAVED_ITEMS.put(this.getUuid(), soulboundItems);
        }
    }

    private void scanAndSave(List<ItemStack> list, List<ItemStack> savedList, int reqLevel) {
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = list.get(i);
            if (stack.isEmpty()) continue;

            NbtCompound nbt = stack.getNbt();
            if (nbt != null && nbt.contains("passive_level")) {
                int level = nbt.getInt("passive_level");

                if (level >= reqLevel) {
                    // Copy to safety
                    savedList.add(stack.copy());
                    // Delete from inventory so it doesn't drop
                    list.set(i, ItemStack.EMPTY);
                }
            }
        }
    }
}