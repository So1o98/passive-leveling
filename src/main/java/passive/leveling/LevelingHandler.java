package passive.leveling;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import java.util.*;

public class LevelingHandler {

    private static final Map<UUID, XpAccumulator> XP_CACHE = new HashMap<>();
    private static final long COMBO_TIMEOUT_MS = 3000;

    private static class XpAccumulator {
        long lastTime;
        int total;
    }

    public void addXp(World world, PlayerEntity player, ItemStack stack, int amount) {
        if (world.isClient) return;
        if (!isLevelable(stack.getItem())) return;

        fixEnchantments(stack);

        NbtCompound nbt = stack.getOrCreateNbt();
        int currentLevel = nbt.getInt("passive_level");
        if (currentLevel < 1) currentLevel = 1;

        // 1. CHECK EVOLUTION CAP (Tier Cap)
        int maxLevelForTier = getMaxLevelForTier(stack.getItem());
        if (maxLevelForTier != -1 && currentLevel >= maxLevelForTier) {
            player.sendMessage(Text.literal("⚠ Tool Ready! Shift-Right-Click a Crafting Table to evolve!").formatted(Formatting.AQUA, Formatting.BOLD), true);
            return;
        }

        // 2. CHECK PRESTIGE CAP (Prestige Requirement)
        if (PassiveLeveling.CONFIG != null) {
            int prestigeReq = PassiveLeveling.CONFIG.leveling.prestige_level_requirement;
            if (currentLevel >= prestigeReq) {
                player.sendMessage(Text.literal("MAX LEVEL - Ready to Prestige!").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), true);
                return;
            }
        }

        int currentXp = nbt.getInt("passive_xp");
        currentXp += amount;

        boolean leveledUp = false;
        while (currentXp >= getRequiredXpForNextLevel(currentLevel)) {
            currentXp -= getRequiredXpForNextLevel(currentLevel);
            currentLevel++;
            leveledUp = true;
            applyRewards(player, stack, currentLevel);
        }

        nbt.putInt("passive_xp", currentXp);
        nbt.putInt("passive_level", currentLevel);

        if (leveledUp) {
            XP_CACHE.remove(player.getUuid());
            player.sendMessage(Text.literal("Level Up! Your tool is now Level " + currentLevel).formatted(Formatting.GREEN, Formatting.BOLD), true);
            player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5f, 1.0f);

            // --- GIVE RANDOM MONEY ON LEVEL UP ---
            if (PassiveLeveling.CONFIG != null) {
                int min = PassiveLeveling.CONFIG.rewards.min_level_up_coins;
                int max = PassiveLeveling.CONFIG.rewards.max_level_up_coins;

                // Safety check: ensure min isn't larger than max
                if (min > max) min = max;

                // Random between min (inclusive) and max (inclusive)
                int amountToGive = player.getRandom().nextBetween(min, max);

                if (amountToGive > 0) {
                    giveNumismaticReward(player, "bronze_coin", amountToGive);
                }
            }

            checkEvolution(world, player, stack, currentLevel);
        } else {
            handleXpDisplay(player, amount);
        }
    }

    private void applyRewards(PlayerEntity player, ItemStack stack, int newLevel) {
        if (PassiveLeveling.CONFIG == null) return;

        if (stack.getItem() == Items.BOW || stack.getItem() == Items.CROSSBOW || stack.getItem() == Items.TRIDENT) {
            if (newLevel % PassiveLeveling.CONFIG.leveling.bow_enchant_interval == 0) {
                applyRandomEnchantment(player, stack);
            }
        }
        else if (stack.getItem() == Items.FISHING_ROD) {
            if (newLevel % PassiveLeveling.CONFIG.leveling.rod_enchant_interval == 0) {
                applyRandomEnchantment(player, stack);
            }
        }
    }

    public static void applyRandomEnchantment(PlayerEntity player, ItemStack stack) {
        if (!PassiveLeveling.CONFIG.leveling.enable_level_up_enchantments) return;

        int prestige = 0;
        if (stack.hasNbt() && stack.getNbt().contains("passive_prestige")) {
            prestige = stack.getNbt().getInt("passive_prestige");
        }

        List<Enchantment> validEnchants = new ArrayList<>();
        for (Enchantment e : Registries.ENCHANTMENT) {
            if (e.isAcceptableItem(stack) && !e.isCursed()) {
                String id = Registries.ENCHANTMENT.getId(e).toString();
                if (!id.contains("curse") && !id.contains("vanishing") && !id.contains("binding")) {
                    validEnchants.add(e);
                }
            }
        }

        if (validEnchants.isEmpty()) return;

        Enchantment chosen = validEnchants.get(player.getRandom().nextInt(validEnchants.size()));
        int newLevel = chosen.getMaxLevel() + prestige;
        if (newLevel > 50) newLevel = 50;

        Map<Enchantment, Integer> currentEnchants = EnchantmentHelper.get(stack);
        currentEnchants.put(chosen, newLevel);

        stack.removeSubNbt("Enchantments");
        EnchantmentHelper.set(currentEnchants, stack);

        boolean isIllegal = newLevel > chosen.getMaxLevel();
        Formatting color = isIllegal ? Formatting.LIGHT_PURPLE : Formatting.GREEN;
        String extra = isIllegal ? " (Overcharged!)" : "";

        player.sendMessage(Text.literal("✦ Imbued with " + chosen.getName(1).getString() + " " + newLevel + extra).formatted(color), true);
        player.playSound(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
    }

    public static void checkEvolution(World world, PlayerEntity player, ItemStack stack, int level) {
        if (PassiveLeveling.CONFIG == null) return;
        Item nextItem = getNextEvolution(stack.getItem(), level);
        if (nextItem == null) return;

        if (PassiveLeveling.CONFIG.leveling.require_smithing_for_evolution) {
            player.sendMessage(Text.literal("⚠ Tool Ready! Shift+Right-Click a Crafting Table to upgrade.").formatted(Formatting.AQUA, Formatting.BOLD), true);
            player.playSound(SoundEvents.BLOCK_ANVIL_USE, 0.5f, 1.0f);
        } else {
            ItemStack newStack = new ItemStack(nextItem);
            if (stack.hasNbt()) newStack.setNbt(stack.getNbt().copy());
            Map<Enchantment, Integer> enchants = EnchantmentHelper.get(stack);
            EnchantmentHelper.set(enchants, newStack);
            player.setStackInHand(Hand.MAIN_HAND, newStack);
            TierUnlocker.onUnlock(player, nextItem);
            applyRandomEnchantment(player, newStack);

            // --- GIVE CONFIGURABLE BONUS MONEY ON EVOLUTION ---
            if (PassiveLeveling.CONFIG != null) {
                int silverAmount = PassiveLeveling.CONFIG.rewards.tier_upgrade_silver_coins;
                if (silverAmount > 0) {
                    giveNumismaticReward(player, "silver_coin", silverAmount);
                }
            }

            player.sendMessage(Text.literal("Your tool has evolved into " + nextItem.getName().getString() + "!").formatted(Formatting.AQUA, Formatting.BOLD), true);
            player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            if (world.getServer() != null) {
                world.getServer().getPlayerManager().broadcast(
                        Text.literal(player.getName().getString() + " has upgraded their tool to " + nextItem.getName().getString() + "!").formatted(Formatting.YELLOW),
                        false
                );
            }
        }
    }

    private static void giveNumismaticReward(PlayerEntity player, String coinType, int count) {
        try {
            Item coin = Registries.ITEM.get(new Identifier("numismatic-overhaul", coinType));
            if (coin != Items.AIR) {
                ItemStack coins = new ItemStack(coin, count);
                if (!player.getInventory().insertStack(coins)) {
                    player.dropItem(coins, false);
                }
            }
        } catch (Exception e) {
            // Ignore if mod is missing
        }
    }

    private void handleXpDisplay(PlayerEntity player, int amount) {
        long now = System.currentTimeMillis();
        UUID id = player.getUuid();
        XpAccumulator acc = XP_CACHE.computeIfAbsent(id, k -> new XpAccumulator());
        if (now - acc.lastTime > COMBO_TIMEOUT_MS) acc.total = amount;
        else acc.total += amount;
        acc.lastTime = now;
        player.sendMessage(Text.literal("+" + acc.total + " Tool XP").formatted(Formatting.GOLD), true);
    }

    private void fixEnchantments(ItemStack stack) {
        if (!stack.hasEnchantments()) return;
        Map<Enchantment, Integer> cleanMap = EnchantmentHelper.get(stack);
        EnchantmentHelper.set(cleanMap, stack);
    }

    public static int getRequiredXpForNextLevel(int level) {
        if (PassiveLeveling.CONFIG == null) return 50 + (level * 10);
        int base = PassiveLeveling.CONFIG.leveling.base_xp_cost;
        float multiplier = PassiveLeveling.CONFIG.leveling.xp_cost_multiplier;
        int addition = PassiveLeveling.CONFIG.leveling.flat_xp_increase;
        return (int) (base * Math.pow(multiplier, level - 1) + (addition * level));
    }

    public int getMaxLevelForTier(Item item) {
        if (PassiveLeveling.CONFIG == null) return -1;
        ModConfig.Leveling config = PassiveLeveling.CONFIG.leveling;

        if (item == Items.WOODEN_PICKAXE) return config.pickaxe_milestones.wood_to_stone;
        if (item == Items.STONE_PICKAXE) return config.pickaxe_milestones.stone_to_iron;
        if (item == Items.IRON_PICKAXE) return config.pickaxe_milestones.iron_to_diamond;
        if (item == Items.DIAMOND_PICKAXE) return config.pickaxe_milestones.diamond_to_netherite;

        if (item == Items.WOODEN_AXE) return config.axe_milestones.wood_to_stone;
        if (item == Items.STONE_AXE) return config.axe_milestones.stone_to_iron;
        if (item == Items.IRON_AXE) return config.axe_milestones.iron_to_diamond;
        if (item == Items.DIAMOND_AXE) return config.axe_milestones.diamond_to_netherite;

        if (item == Items.WOODEN_SHOVEL) return config.shovel_milestones.wood_to_stone;
        if (item == Items.STONE_SHOVEL) return config.shovel_milestones.stone_to_iron;
        if (item == Items.IRON_SHOVEL) return config.shovel_milestones.iron_to_diamond;
        if (item == Items.DIAMOND_SHOVEL) return config.shovel_milestones.diamond_to_netherite;

        if (item == Items.WOODEN_HOE) return config.hoe_milestones.wood_to_stone;
        if (item == Items.STONE_HOE) return config.hoe_milestones.stone_to_iron;
        if (item == Items.IRON_HOE) return config.hoe_milestones.iron_to_diamond;
        if (item == Items.DIAMOND_HOE) return config.hoe_milestones.diamond_to_netherite;

        if (item == Items.WOODEN_SWORD) return config.sword_milestones.wood_to_stone;
        if (item == Items.STONE_SWORD) return config.sword_milestones.stone_to_iron;
        if (item == Items.IRON_SWORD) return config.sword_milestones.iron_to_diamond;
        if (item == Items.DIAMOND_SWORD) return config.sword_milestones.diamond_to_netherite;

        return -1;
    }

    public static Item getNextEvolution(Item item, int level) {
        if (PassiveLeveling.CONFIG == null) return null;
        ModConfig.Leveling config = PassiveLeveling.CONFIG.leveling;

        if (isPickaxe(item)) {
            if (item == Items.WOODEN_PICKAXE && level >= config.pickaxe_milestones.wood_to_stone) return Items.STONE_PICKAXE;
            if (item == Items.STONE_PICKAXE && level >= config.pickaxe_milestones.stone_to_iron) return Items.IRON_PICKAXE;
            if (item == Items.IRON_PICKAXE && level >= config.pickaxe_milestones.iron_to_diamond) return Items.DIAMOND_PICKAXE;
            if (item == Items.DIAMOND_PICKAXE && level >= config.pickaxe_milestones.diamond_to_netherite) return Items.NETHERITE_PICKAXE;
        }
        else if (isSword(item)) {
            if (item == Items.WOODEN_SWORD && level >= config.sword_milestones.wood_to_stone) return Items.STONE_SWORD;
            if (item == Items.STONE_SWORD && level >= config.sword_milestones.stone_to_iron) return Items.IRON_SWORD;
            if (item == Items.IRON_SWORD && level >= config.sword_milestones.iron_to_diamond) return Items.DIAMOND_SWORD;
            if (item == Items.DIAMOND_SWORD && level >= config.sword_milestones.diamond_to_netherite) return Items.NETHERITE_SWORD;
        }
        else if (isAxe(item)) {
            if (item == Items.WOODEN_AXE && level >= config.axe_milestones.wood_to_stone) return Items.STONE_AXE;
            if (item == Items.STONE_AXE && level >= config.axe_milestones.stone_to_iron) return Items.IRON_AXE;
            if (item == Items.IRON_AXE && level >= config.axe_milestones.iron_to_diamond) return Items.DIAMOND_AXE;
            if (item == Items.DIAMOND_AXE && level >= config.axe_milestones.diamond_to_netherite) return Items.NETHERITE_AXE;
        }
        else if (isShovel(item)) {
            if (item == Items.WOODEN_SHOVEL && level >= config.shovel_milestones.wood_to_stone) return Items.STONE_SHOVEL;
            if (item == Items.STONE_SHOVEL && level >= config.shovel_milestones.stone_to_iron) return Items.IRON_SHOVEL;
            if (item == Items.IRON_SHOVEL && level >= config.shovel_milestones.iron_to_diamond) return Items.DIAMOND_SHOVEL;
            if (item == Items.DIAMOND_SHOVEL && level >= config.shovel_milestones.diamond_to_netherite) return Items.NETHERITE_SHOVEL;
        }
        else if (isHoe(item)) {
            if (item == Items.WOODEN_HOE && level >= config.hoe_milestones.wood_to_stone) return Items.STONE_HOE;
            if (item == Items.STONE_HOE && level >= config.hoe_milestones.stone_to_iron) return Items.IRON_HOE;
            if (item == Items.IRON_HOE && level >= config.hoe_milestones.iron_to_diamond) return Items.DIAMOND_HOE;
            if (item == Items.DIAMOND_HOE && level >= config.hoe_milestones.diamond_to_netherite) return Items.NETHERITE_HOE;
        }
        return null;
    }

    public static boolean isLevelable(Item item) {
        return isPickaxe(item) || isSword(item) || isAxe(item) || isShovel(item) || isHoe(item) ||
                item == Items.BOW || item == Items.FISHING_ROD ||
                item == Items.CROSSBOW || item == Items.TRIDENT;
    }
    public static boolean isPickaxe(Item item) { return item == Items.WOODEN_PICKAXE || item == Items.STONE_PICKAXE || item == Items.IRON_PICKAXE || item == Items.DIAMOND_PICKAXE || item == Items.NETHERITE_PICKAXE; }
    public static boolean isSword(Item item) { return item == Items.WOODEN_SWORD || item == Items.STONE_SWORD || item == Items.IRON_SWORD || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD; }
    public static boolean isAxe(Item item) { return item == Items.WOODEN_AXE || item == Items.STONE_AXE || item == Items.IRON_AXE || item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE; }
    public static boolean isShovel(Item item) { return item == Items.WOODEN_SHOVEL || item == Items.STONE_SHOVEL || item == Items.IRON_SHOVEL || item == Items.DIAMOND_SHOVEL || item == Items.NETHERITE_SHOVEL; }
    public static boolean isHoe(Item item) { return item == Items.WOODEN_HOE || item == Items.STONE_HOE || item == Items.IRON_HOE || item == Items.DIAMOND_HOE || item == Items.NETHERITE_HOE; }
}