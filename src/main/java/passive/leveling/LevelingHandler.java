package passive.leveling;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LevelingHandler {

    public static PassiveLevelingConfig getConfig() {
        return AutoConfig.getConfigHolder(PassiveLevelingConfig.class).getConfig();
    }

    public static int getStartingLevel(Item item) {
        PassiveLevelingConfig config = getConfig();
        PassiveLevelingConfig.ToolMilestones milestones = null;
        if (item instanceof PickaxeItem) milestones = config.pickaxe;
        else if (item instanceof AxeItem) milestones = config.axe;
        else if (item instanceof ShovelItem) milestones = config.shovel;
        else if (item instanceof SwordItem) milestones = config.sword;
        else if (item instanceof HoeItem) milestones = config.hoe;

        if (milestones == null) return 0;

        if (isNetherite(item)) return milestones.netheriteLevel;
        if (isDiamond(item)) return milestones.diamondLevel;
        if (isIron(item)) return milestones.ironLevel;
        if (isStone(item)) return milestones.stoneLevel;
        return 0;
    }

    private static boolean isNetherite(Item item) {
        return item == Items.NETHERITE_PICKAXE || item == Items.NETHERITE_AXE ||
                item == Items.NETHERITE_SHOVEL || item == Items.NETHERITE_HOE ||
                item == Items.NETHERITE_SWORD;
    }
    private static boolean isDiamond(Item item) {
        return item == Items.DIAMOND_PICKAXE || item == Items.DIAMOND_AXE ||
                item == Items.DIAMOND_SHOVEL || item == Items.DIAMOND_HOE ||
                item == Items.DIAMOND_SWORD;
    }
    private static boolean isIron(Item item) {
        return item == Items.IRON_PICKAXE || item == Items.IRON_AXE ||
                item == Items.IRON_SHOVEL || item == Items.IRON_HOE ||
                item == Items.IRON_SWORD;
    }
    private static boolean isStone(Item item) {
        return item == Items.STONE_PICKAXE || item == Items.STONE_AXE ||
                item == Items.STONE_SHOVEL || item == Items.STONE_HOE ||
                item == Items.STONE_SWORD;
    }

    public static int getBlockXp(BlockState state) {
        PassiveLevelingConfig config = getConfig();
        if (state.isIn(BlockTags.LOGS)) return config.woodXp;
        if (state.isIn(BlockTags.COAL_ORES)) return config.coalXp;
        if (state.isIn(BlockTags.COPPER_ORES)) return config.copperXp;
        if (state.isIn(BlockTags.IRON_ORES)) return config.ironXp;
        if (state.isIn(BlockTags.GOLD_ORES)) return config.goldXp;
        if (state.isIn(BlockTags.REDSTONE_ORES)) return config.redstoneXp;
        if (state.isIn(BlockTags.LAPIS_ORES)) return config.lapisXp;
        if (state.isIn(BlockTags.DIAMOND_ORES)) return config.diamondXp;
        if (state.isIn(BlockTags.EMERALD_ORES)) return config.emeraldXp;
        if (state.getBlock().getTranslationKey().contains("quartz_ore")) return config.quartzXp;
        return config.stoneXp;
    }

    public static int getEntityXp(LivingEntity entity) {
        PassiveLevelingConfig config = getConfig();
        if (entity instanceof EnderDragonEntity || entity instanceof WitherEntity) return config.bossMobXp;
        if (entity instanceof HostileEntity) return config.hostileMobXp;
        return config.passiveMobXp;
    }

    private static final Map<UUID, ComboSession> XP_SESSIONS = new HashMap<>();
    private static final int COMBO_TIMEOUT_TICKS = 60;

    private static class ComboSession {
        long lastTick;
        int accumulatedXp;
        Item toolUsed;
        ComboSession(long tick, int xp, Item tool) {
            this.lastTick = tick;
            this.accumulatedXp = xp;
            this.toolUsed = tool;
        }
    }

    public static void addXp(ServerPlayerEntity player, ItemStack stack, int amount) {
        addXp(player, stack, amount, true);
    }

    public static void addXp(ServerPlayerEntity player, ItemStack stack, int amount, boolean showMessage) {
        LevelingData data = stack.getOrDefault(PassiveLeveling.LEVEL_DATA, LevelingData.DEFAULT);
        PassiveLevelingConfig config = getConfig();
        int currentLevel = data.level();
        int maxLevel;

        if (stack.getItem() instanceof FishingRodItem) maxLevel = config.fishingEnchantInterval;
        else if (isRanged(stack.getItem())) maxLevel = config.rangedEnchantInterval;
        else maxLevel = config.prestigeLevelRequirement;

        if (currentLevel >= maxLevel) return;

        if (!isRanged(stack.getItem())) {
            Item targetItem = getTargetItemForLevel(stack.getItem(), currentLevel);
            if (targetItem != null && targetItem != stack.getItem()) {
                if (showMessage) {
                    player.sendMessage(Text.literal("XP Capped! Shift-Right-Click a Crafting Table to evolve!").formatted(Formatting.RED), true);
                }
                return;
            }
        }

        if (data.level() == 0 && data.xp() == 0 && data.prestige() == 0) {
            int startLevel = getStartingLevel(stack.getItem());
            data = new LevelingData(startLevel, 0, 0);
        }

        long currentTick = player.getWorld().getTime();
        int displayAmount = amount;
        UUID id = player.getUuid();

        if (XP_SESSIONS.containsKey(id)) {
            ComboSession session = XP_SESSIONS.get(id);
            if (currentTick - session.lastTick < COMBO_TIMEOUT_TICKS && session.toolUsed == stack.getItem()) {
                session.accumulatedXp += amount;
                session.lastTick = currentTick;
                displayAmount = session.accumulatedXp;
            } else {
                session.accumulatedXp = amount;
                session.lastTick = currentTick;
                session.toolUsed = stack.getItem();
            }
        } else {
            XP_SESSIONS.put(id, new ComboSession(currentTick, amount, stack.getItem()));
        }

        if (showMessage) {
            player.sendMessage(Text.literal("+" + displayAmount + " XP").formatted(Formatting.GREEN), true);
        }

        int newXp = data.xp() + amount;
        boolean leveledUp = false;

        while (true) {
            int xpRequired = getMaxXpForLevel(currentLevel);

            if (newXp < xpRequired || currentLevel >= maxLevel) {
                break;
            }

            if (!isRanged(stack.getItem())) {
                Item targetForNextLevel = getTargetItemForLevel(stack.getItem(), currentLevel + 1);
                if (targetForNextLevel != null && targetForNextLevel != stack.getItem()) {
                    currentLevel++;
                    newXp = 0;
                    leveledUp = true;
                    break;
                }
            }

            newXp -= xpRequired;
            currentLevel++;
            leveledUp = true;
        }

        if (leveledUp) {
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.0f);

            Item targetItem = getTargetItemForLevel(stack.getItem(), currentLevel);
            if (targetItem != null && targetItem != stack.getItem()) {
                player.sendMessage(Text.literal("Upgrade Available! Shift-Right-Click a Crafting Table to evolve!").formatted(Formatting.AQUA, Formatting.BOLD), false);
            }

            if (currentLevel >= maxLevel) {
                currentLevel = maxLevel;
                newXp = 0;
                player.sendMessage(Text.literal("MAX LEVEL! Shift-Right-Click a Smithing Table to Prestige!").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), false);
            } else {
                player.sendMessage(Text.literal("Level Up! (" + currentLevel + ")").formatted(Formatting.GOLD), true);
            }
        }

        stack.set(PassiveLeveling.LEVEL_DATA, new LevelingData(currentLevel, newXp, data.prestige()));
        player.getInventory().markDirty();
    }

    public static int getMaxXpForLevel(int level) {
        PassiveLevelingConfig config = getConfig();
        double calculated = (config.baseXpCost + (level * config.xpLinearIncrease)) * Math.pow(config.xpExponentialMultiplier, level);
        return (int) Math.max(1, calculated);
    }

    public static void validateToolTier(ServerPlayerEntity player, ItemStack stack) {
        LevelingData data = stack.getOrDefault(PassiveLeveling.LEVEL_DATA, LevelingData.DEFAULT);
        if (isRanged(stack.getItem())) return;
        Item target = getTargetItemForLevel(stack.getItem(), data.level());
        if (target != null && target != stack.getItem()) {

            // FIX: Unlock recipe before swapping
            RecipeUnlockHandler.checkAndUnlock(player);

            swapTool(player, stack, target, data.level(), data.prestige());
        }
    }

    public static boolean tryEvolve(ServerPlayerEntity player, ItemStack stack) {
        LevelingData data = stack.getOrDefault(PassiveLeveling.LEVEL_DATA, LevelingData.DEFAULT);

        if (!isRanged(stack.getItem())) {
            Item targetItem = getTargetItemForLevel(stack.getItem(), data.level());

            if (targetItem != null && targetItem != stack.getItem()) {
                player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 0.5f);
                spawnFirework(player);

                Text message = Text.literal(player.getName().getString() + " evolved their tool to ").formatted(Formatting.YELLOW)
                        .append(Text.translatable(targetItem.getTranslationKey()).formatted(Formatting.AQUA, Formatting.BOLD))
                        .append(Text.literal("!"));
                player.getServer().getPlayerManager().broadcast(message, false);

                // FIX: Unlock recipe before swapping
                RecipeUnlockHandler.checkAndUnlock(player);

                swapTool(player, stack, targetItem, data.level(), data.prestige());
                return true;
            }
        }
        return false;
    }

    public static boolean handlePrestigeInteraction(ServerPlayerEntity player, ItemStack stack) {
        LevelingData data = stack.getOrDefault(PassiveLeveling.LEVEL_DATA, LevelingData.DEFAULT);
        PassiveLevelingConfig config = getConfig();

        int maxLevel = isRanged(stack.getItem()) ? config.rangedEnchantInterval : config.prestigeLevelRequirement;
        if (stack.getItem() instanceof FishingRodItem) maxLevel = config.fishingEnchantInterval;

        if (data.level() >= maxLevel) {
            return tryPrestige(player, stack);
        }
        return false;
    }

    private static boolean tryPrestige(ServerPlayerEntity player, ItemStack stack) {
        LevelingData data = stack.getOrDefault(PassiveLeveling.LEVEL_DATA, LevelingData.DEFAULT);
        PassiveLevelingConfig config = getConfig();
        boolean wasSoulbound = data.level() >= config.soulboundLevel;
        int newPrestige = data.prestige() + 1;

        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.0f);
        spawnPrestigeLightning(player);

        Text message = Text.literal(player.getName().getString() + " reached ").formatted(Formatting.LIGHT_PURPLE)
                .append(Text.literal("PRESTIGE " + newPrestige).formatted(Formatting.RED, Formatting.BOLD))
                .append(Text.literal(" on their tool!"));
        player.getServer().getPlayerManager().broadcast(message, false);

        int startingLevel = wasSoulbound ? config.soulboundLevel : 0;

        if (isRanged(stack.getItem())) {
            stack.set(PassiveLeveling.LEVEL_DATA, new LevelingData(startingLevel, 0, newPrestige));
            applyRandomEnchantment(player, stack, false);
        } else {
            Item baseItem = getBaseItem(stack.getItem());
            swapTool(player, stack, baseItem, startingLevel, newPrestige);
            applyRandomEnchantment(player, player.getMainHandStack(), true);
            applyRandomEnchantment(player, player.getMainHandStack(), true);
        }
        return true;
    }

    private static void spawnPrestigeLightning(ServerPlayerEntity player) {
        LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(player.getWorld());
        if (lightning != null) {
            lightning.refreshPositionAfterTeleport(player.getX(), player.getY(), player.getZ());
            lightning.setCosmetic(true);
            player.getWorld().spawnEntity(lightning);
        }
    }

    private static void spawnFirework(ServerPlayerEntity player) {
        ItemStack rocketItem = new ItemStack(Items.FIREWORK_ROCKET);
        FireworkExplosionComponent explosion = new FireworkExplosionComponent(
                FireworkExplosionComponent.Type.LARGE_BALL,
                IntList.of(0x00FFFF, 0xFFFFFF),
                IntList.of(0xFFD700),
                true,
                true
        );
        rocketItem.set(DataComponentTypes.FIREWORKS, new FireworksComponent(1, List.of(explosion)));
        FireworkRocketEntity rocketEntity = new FireworkRocketEntity(player.getWorld(), player.getX(), player.getY(), player.getZ(), rocketItem);
        player.getWorld().spawnEntity(rocketEntity);
    }

    private static void swapTool(ServerPlayerEntity player, ItemStack oldStack, Item newItem, int level, int prestige) {
        ItemStack newStack = new ItemStack(newItem);
        newStack.set(DataComponentTypes.ENCHANTMENTS, oldStack.getEnchantments());
        newStack.set(PassiveLeveling.LEVEL_DATA, new LevelingData(level, 0, prestige));
        if (getConfig().enableTierIncreaseEnchants) applyRandomEnchantment(player, newStack, false);
        player.getInventory().setStack(player.getInventory().selectedSlot, newStack);
        player.getInventory().markDirty();
    }

    private static Item getTargetItemForLevel(Item current, int level) {
        PassiveLevelingConfig config = getConfig();
        PassiveLevelingConfig.ToolMilestones milestones;
        if (current instanceof PickaxeItem) milestones = config.pickaxe;
        else if (current instanceof AxeItem) milestones = config.axe;
        else if (current instanceof ShovelItem) milestones = config.shovel;
        else if (current instanceof SwordItem) milestones = config.sword;
        else if (current instanceof HoeItem) milestones = config.hoe;
        else return null;

        Item targetMaterial;
        if (level >= milestones.netheriteLevel) targetMaterial = Items.NETHERITE_PICKAXE;
        else if (level >= milestones.diamondLevel) targetMaterial = Items.DIAMOND_PICKAXE;
        else if (level >= milestones.ironLevel) targetMaterial = Items.IRON_PICKAXE;
        else if (level >= milestones.stoneLevel) targetMaterial = Items.STONE_PICKAXE;
        else targetMaterial = Items.WOODEN_PICKAXE;

        if (current instanceof PickaxeItem) return targetMaterial;
        if (current instanceof AxeItem) {
            if (targetMaterial == Items.NETHERITE_PICKAXE) return Items.NETHERITE_AXE;
            if (targetMaterial == Items.DIAMOND_PICKAXE) return Items.DIAMOND_AXE;
            if (targetMaterial == Items.IRON_PICKAXE) return Items.IRON_AXE;
            if (targetMaterial == Items.STONE_PICKAXE) return Items.STONE_AXE;
            return Items.WOODEN_AXE;
        }
        if (current instanceof SwordItem) {
            if (targetMaterial == Items.NETHERITE_PICKAXE) return Items.NETHERITE_SWORD;
            if (targetMaterial == Items.DIAMOND_PICKAXE) return Items.DIAMOND_SWORD;
            if (targetMaterial == Items.IRON_PICKAXE) return Items.IRON_SWORD;
            if (targetMaterial == Items.STONE_PICKAXE) return Items.STONE_SWORD;
            return Items.WOODEN_SWORD;
        }
        if (current instanceof ShovelItem) {
            if (targetMaterial == Items.NETHERITE_PICKAXE) return Items.NETHERITE_SHOVEL;
            if (targetMaterial == Items.DIAMOND_PICKAXE) return Items.DIAMOND_SHOVEL;
            if (targetMaterial == Items.IRON_PICKAXE) return Items.IRON_SHOVEL;
            if (targetMaterial == Items.STONE_PICKAXE) return Items.STONE_SHOVEL;
            return Items.WOODEN_SHOVEL;
        }
        if (current instanceof HoeItem) {
            if (targetMaterial == Items.NETHERITE_PICKAXE) return Items.NETHERITE_HOE;
            if (targetMaterial == Items.DIAMOND_PICKAXE) return Items.DIAMOND_HOE;
            if (targetMaterial == Items.IRON_PICKAXE) return Items.IRON_HOE;
            if (targetMaterial == Items.STONE_PICKAXE) return Items.STONE_HOE;
            return Items.WOODEN_HOE;
        }
        return null;
    }

    private static Item getBaseItem(Item item) {
        if (item instanceof PickaxeItem) return Items.WOODEN_PICKAXE;
        if (item instanceof AxeItem) return Items.WOODEN_AXE;
        if (item instanceof ShovelItem) return Items.WOODEN_SHOVEL;
        if (item instanceof HoeItem) return Items.WOODEN_HOE;
        return Items.WOODEN_SWORD;
    }

    private static boolean isRanged(Item item) {
        return item instanceof BowItem || item instanceof CrossbowItem ||
                item instanceof TridentItem || item instanceof FishingRodItem;
    }

    private static void applyRandomEnchantment(ServerPlayerEntity player, ItemStack stack, boolean isIllegal) {
        var registry = player.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        List<RegistryEntry<Enchantment>> validEnchants = new ArrayList<>();
        registry.streamEntries().forEach(entry -> {
            if (entry.value().isAcceptableItem(stack) && !entry.isIn(EnchantmentTags.CURSE)) {
                validEnchants.add(entry);
            }
        });
        if (validEnchants.isEmpty()) return;
        RegistryEntry<Enchantment> randomEnchant = validEnchants.get(player.getRandom().nextInt(validEnchants.size()));
        int currentLvl = stack.getEnchantments().getLevel(randomEnchant);
        int newLvl = isIllegal ? 5 + player.getRandom().nextInt(6) : Math.min(currentLvl + 1, randomEnchant.value().getMaxLevel());
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(stack.getEnchantments());
        builder.set(randomEnchant, newLvl);
        stack.set(DataComponentTypes.ENCHANTMENTS, builder.build());
    }
}