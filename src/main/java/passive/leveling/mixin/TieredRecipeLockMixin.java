package passive.leveling.mixin;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import passive.leveling.LevelingData;
import passive.leveling.LevelingHandler;
import passive.leveling.PassiveLeveling;
import passive.leveling.PassiveLevelingConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mixin(ServerPlayerEntity.class)
public class TieredRecipeLockMixin {

    @ModifyVariable(method = "unlockRecipes(Ljava/util/Collection;)I", at = @At("HEAD"), argsOnly = true)
    private Collection<RecipeEntry<?>> filterRecipes(Collection<RecipeEntry<?>> recipes) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        PassiveLevelingConfig config = LevelingHandler.getConfig();

        // If locking is disabled, allow everything
        if (!config.lockRecipesToTier) return recipes;

        List<RecipeEntry<?>> filtered = new ArrayList<>(recipes);

        // Remove recipes if the player does NOT meet the requirements
        filtered.removeIf(recipe -> shouldBlock(player, recipe.id().toString(), config));

        return filtered;
    }

    private boolean shouldBlock(ServerPlayerEntity player, String id, PassiveLevelingConfig config) {
        // --- PICKAXES ---
        if (id.contains("stone_pickaxe")) return !hasTier(player, Items.WOODEN_PICKAXE, config.pickaxe.stoneLevel);
        if (id.contains("iron_pickaxe")) return !hasTier(player, Items.STONE_PICKAXE, config.pickaxe.ironLevel);
        if (id.contains("diamond_pickaxe")) return !hasTier(player, Items.IRON_PICKAXE, config.pickaxe.diamondLevel);
        if (id.contains("netherite_pickaxe")) return !hasTier(player, Items.DIAMOND_PICKAXE, config.pickaxe.netheriteLevel);

        // --- AXES ---
        if (id.contains("stone_axe")) return !hasTier(player, Items.WOODEN_AXE, config.axe.stoneLevel);
        if (id.contains("iron_axe")) return !hasTier(player, Items.STONE_AXE, config.axe.ironLevel);
        if (id.contains("diamond_axe")) return !hasTier(player, Items.IRON_AXE, config.axe.diamondLevel);
        if (id.contains("netherite_axe")) return !hasTier(player, Items.DIAMOND_AXE, config.axe.netheriteLevel);

        // --- SHOVELS ---
        if (id.contains("stone_shovel")) return !hasTier(player, Items.WOODEN_SHOVEL, config.shovel.stoneLevel);
        if (id.contains("iron_shovel")) return !hasTier(player, Items.STONE_SHOVEL, config.shovel.ironLevel);
        if (id.contains("diamond_shovel")) return !hasTier(player, Items.IRON_SHOVEL, config.shovel.diamondLevel);
        if (id.contains("netherite_shovel")) return !hasTier(player, Items.DIAMOND_SHOVEL, config.shovel.netheriteLevel);

        // --- SWORDS ---
        if (id.contains("stone_sword")) return !hasTier(player, Items.WOODEN_SWORD, config.sword.stoneLevel);
        if (id.contains("iron_sword")) return !hasTier(player, Items.STONE_SWORD, config.sword.ironLevel);
        if (id.contains("diamond_sword")) return !hasTier(player, Items.IRON_SWORD, config.sword.diamondLevel);
        if (id.contains("netherite_sword")) return !hasTier(player, Items.DIAMOND_SWORD, config.sword.netheriteLevel);

        // --- HOES ---
        if (id.contains("stone_hoe")) return !hasTier(player, Items.WOODEN_HOE, config.hoe.stoneLevel);
        if (id.contains("iron_hoe")) return !hasTier(player, Items.STONE_HOE, config.hoe.ironLevel);
        if (id.contains("diamond_hoe")) return !hasTier(player, Items.IRON_HOE, config.hoe.diamondLevel);
        if (id.contains("netherite_hoe")) return !hasTier(player, Items.DIAMOND_HOE, config.hoe.netheriteLevel);

        return false; // Allow all other recipes
    }

    private boolean hasTier(ServerPlayerEntity player, Item requiredItem, int requiredLevel) {
        // 1. Check Main Hand
        if (checkStack(player.getMainHandStack(), requiredItem, requiredLevel)) return true;

        // 2. Check Inventory
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (checkStack(player.getInventory().getStack(i), requiredItem, requiredLevel)) return true;
        }
        return false;
    }

    private boolean checkStack(ItemStack stack, Item requiredItem, int requiredLevel) {
        if (stack.getItem() == requiredItem && stack.contains(PassiveLeveling.LEVEL_DATA)) {
            LevelingData data = stack.get(PassiveLeveling.LEVEL_DATA);
            return data.level() >= requiredLevel;
        }
        return false;
    }
}