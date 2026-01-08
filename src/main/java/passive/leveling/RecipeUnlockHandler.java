package passive.leveling;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Collections;

public class RecipeUnlockHandler {

    // --- LOGIC: Used by Mixins to physically block crafting ---
    public static boolean isRecipeLocked(ServerPlayerEntity player, ItemStack stack) {
        PassiveLevelingConfig config = LevelingHandler.getConfig();
        if (!config.lockRecipesToTier) return false;

        Item item = stack.getItem();
        boolean locked = false;
        String reason = "";

        // CHECK PICKAXES
        if (item == Items.STONE_PICKAXE) {
            locked = !hasRequirement(player, Items.WOODEN_PICKAXE, config.pickaxe.stoneLevel)
                    && !hasAny(player, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE);
            reason = "Need Wooden Pickaxe Lvl " + config.pickaxe.stoneLevel;
        }
        else if (item == Items.IRON_PICKAXE) {
            locked = !hasRequirement(player, Items.STONE_PICKAXE, config.pickaxe.ironLevel)
                    && !hasAny(player, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE);
            reason = "Need Stone Pickaxe Lvl " + config.pickaxe.ironLevel;
        }
        else if (item == Items.DIAMOND_PICKAXE) {
            locked = !hasRequirement(player, Items.IRON_PICKAXE, config.pickaxe.diamondLevel)
                    && !hasAny(player, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE);
            reason = "Need Iron Pickaxe Lvl " + config.pickaxe.diamondLevel;
        }
        else if (item == Items.NETHERITE_PICKAXE) {
            locked = !hasRequirement(player, Items.DIAMOND_PICKAXE, config.pickaxe.netheriteLevel)
                    && !hasAny(player, Items.NETHERITE_PICKAXE);
            reason = "Need Diamond Pickaxe Lvl " + config.pickaxe.netheriteLevel;
        }

        // CHECK AXES
        else if (item == Items.STONE_AXE) {
            locked = !hasRequirement(player, Items.WOODEN_AXE, config.axe.stoneLevel)
                    && !hasAny(player, Items.STONE_AXE, Items.IRON_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE);
        }
        else if (item == Items.IRON_AXE) {
            locked = !hasRequirement(player, Items.STONE_AXE, config.axe.ironLevel)
                    && !hasAny(player, Items.IRON_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE);
        }
        else if (item == Items.DIAMOND_AXE) {
            locked = !hasRequirement(player, Items.IRON_AXE, config.axe.diamondLevel)
                    && !hasAny(player, Items.DIAMOND_AXE, Items.NETHERITE_AXE);
        }
        else if (item == Items.NETHERITE_AXE) {
            locked = !hasRequirement(player, Items.DIAMOND_AXE, config.axe.netheriteLevel)
                    && !hasAny(player, Items.NETHERITE_AXE);
        }

        // CHECK SHOVELS
        else if (item == Items.STONE_SHOVEL) {
            locked = !hasRequirement(player, Items.WOODEN_SHOVEL, config.shovel.stoneLevel)
                    && !hasAny(player, Items.STONE_SHOVEL, Items.IRON_SHOVEL, Items.DIAMOND_SHOVEL, Items.NETHERITE_SHOVEL);
        }
        else if (item == Items.IRON_SHOVEL) {
            locked = !hasRequirement(player, Items.STONE_SHOVEL, config.shovel.ironLevel)
                    && !hasAny(player, Items.IRON_SHOVEL, Items.DIAMOND_SHOVEL, Items.NETHERITE_SHOVEL);
        }
        else if (item == Items.DIAMOND_SHOVEL) {
            locked = !hasRequirement(player, Items.IRON_SHOVEL, config.shovel.diamondLevel)
                    && !hasAny(player, Items.DIAMOND_SHOVEL, Items.NETHERITE_SHOVEL);
        }
        else if (item == Items.NETHERITE_SHOVEL) {
            locked = !hasRequirement(player, Items.DIAMOND_SHOVEL, config.shovel.netheriteLevel)
                    && !hasAny(player, Items.NETHERITE_SHOVEL);
        }

        // CHECK SWORDS
        else if (item == Items.STONE_SWORD) {
            locked = !hasRequirement(player, Items.WOODEN_SWORD, config.sword.stoneLevel)
                    && !hasAny(player, Items.STONE_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD);
        }
        else if (item == Items.IRON_SWORD) {
            locked = !hasRequirement(player, Items.STONE_SWORD, config.sword.ironLevel)
                    && !hasAny(player, Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD);
        }
        else if (item == Items.DIAMOND_SWORD) {
            locked = !hasRequirement(player, Items.IRON_SWORD, config.sword.diamondLevel)
                    && !hasAny(player, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD);
        }
        else if (item == Items.NETHERITE_SWORD) {
            locked = !hasRequirement(player, Items.DIAMOND_SWORD, config.sword.netheriteLevel)
                    && !hasAny(player, Items.NETHERITE_SWORD);
        }

        // CHECK HOES
        else if (item == Items.STONE_HOE) {
            locked = !hasRequirement(player, Items.WOODEN_HOE, config.hoe.stoneLevel)
                    && !hasAny(player, Items.STONE_HOE, Items.IRON_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE);
        }
        else if (item == Items.IRON_HOE) {
            locked = !hasRequirement(player, Items.STONE_HOE, config.hoe.ironLevel)
                    && !hasAny(player, Items.IRON_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE);
        }
        else if (item == Items.DIAMOND_HOE) {
            locked = !hasRequirement(player, Items.IRON_HOE, config.hoe.diamondLevel)
                    && !hasAny(player, Items.DIAMOND_HOE, Items.NETHERITE_HOE);
        }
        else if (item == Items.NETHERITE_HOE) {
            locked = !hasRequirement(player, Items.DIAMOND_HOE, config.hoe.netheriteLevel)
                    && !hasAny(player, Items.NETHERITE_HOE);
        }

        if (locked && !reason.isEmpty()) {
            System.out.println("[PassiveLeveling] Blocking Crafting of " + item.getName().getString() + ". Reason: " + reason);
        }

        return locked;
    }

    public static void checkAndUnlock(ServerPlayerEntity player) {
        PassiveLevelingConfig config = LevelingHandler.getConfig();
        if (!config.lockRecipesToTier) return;

        handleChain(player, config.pickaxe, Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE);
        handleChain(player, config.axe, Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE);
        handleChain(player, config.shovel, Items.WOODEN_SHOVEL, Items.STONE_SHOVEL, Items.IRON_SHOVEL, Items.DIAMOND_SHOVEL, Items.NETHERITE_SHOVEL);
        handleChain(player, config.sword, Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD);
        handleChain(player, config.hoe, Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE);
    }

    private static void handleChain(ServerPlayerEntity player, PassiveLevelingConfig.ToolMilestones milestones, Item... hierarchy) {
        for (int i = 0; i < hierarchy.length - 1; i++) {
            Item currentTierItem = hierarchy[i];
            Item nextTierItem = hierarchy[i + 1];

            int requiredLevel;
            if (i == 0) requiredLevel = milestones.stoneLevel;
            else if (i == 1) requiredLevel = milestones.ironLevel;
            else if (i == 2) requiredLevel = milestones.diamondLevel;
            else requiredLevel = milestones.netheriteLevel;

            if (hasRequirement(player, currentTierItem, requiredLevel)) {
                Identifier recipeId = nextTierItem.getRegistryEntry().registryKey().getValue();
                unlockRecipe(player, recipeId);
            }
        }
    }

    private static boolean hasRequirement(ServerPlayerEntity player, Item item, int requiredLevel) {
        if (checkStack(player.getMainHandStack(), item, requiredLevel)) return true;
        if (checkStack(player.getOffHandStack(), item, requiredLevel)) return true;
        if (checkStack(player.currentScreenHandler.getCursorStack(), item, requiredLevel)) return true;

        for (int i = 0; i < player.getInventory().size(); i++) {
            if (checkStack(player.getInventory().getStack(i), item, requiredLevel)) return true;
        }
        return false;
    }

    private static boolean checkStack(ItemStack stack, Item requiredItem, int requiredLevel) {
        if (stack.getItem() == requiredItem && stack.contains(PassiveLeveling.LEVEL_DATA)) {
            LevelingData data = stack.get(PassiveLeveling.LEVEL_DATA);
            return data.level() >= requiredLevel;
        }
        return false;
    }

    // Helper to check if player already possesses a specific tier of tool (or better)
    private static boolean hasAny(ServerPlayerEntity player, Item... items) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            Item invItem = player.getInventory().getStack(i).getItem();
            for (Item allowed : items) {
                if (invItem == allowed) return true;
            }
        }
        return false;
    }

    private static void unlockRecipe(ServerPlayerEntity player, Identifier recipeId) {
        player.getServer().getRecipeManager().get(recipeId).ifPresent(recipeEntry -> {
            if (!player.getRecipeBook().contains(recipeEntry)) {
                player.unlockRecipes(Collections.singleton(recipeEntry));
            }
        });
    }
}