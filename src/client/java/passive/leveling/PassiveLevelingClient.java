package passive.leveling;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.*;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PassiveLevelingClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {

            // 1. GOLD TOOL LOGIC (Absorb Instruction Only)
            if (isGoldTool(stack.getItem())) {
                lines.add(Text.literal("Shift + Right Click in Offhand to Absorb").formatted(Formatting.GOLD));
                return; // Stop here! Do not show XP bar or Level for gold tools.
            }

            // 2. STANDARD LEVELING TOOL LOGIC
            if (isLevelableTool(stack)) {

                // If data is missing (new tool), create a temporary Level 0 view
                LevelingData data = stack.get(PassiveLeveling.LEVEL_DATA);
                if (data == null) {
                    int startLevel = LevelingHandler.getStartingLevel(stack.getItem());
                    data = new LevelingData(startLevel, 0, 0);
                }

                PassiveLevelingConfig config = LevelingHandler.getConfig();

                // Determine Max Level
                int maxLevel;
                if (stack.getItem() instanceof FishingRodItem) {
                    maxLevel = config.fishingEnchantInterval;
                } else if (isRanged(stack.getItem())) {
                    maxLevel = config.rangedEnchantInterval;
                } else {
                    maxLevel = config.prestigeLevelRequirement;
                }

                // Add Rank Line
                if (config.enableTitlePrefixes) {
                    String title = getTitleForLevel(data.level(), maxLevel);
                    lines.add(Text.literal("Rank: " + title).formatted(Formatting.GRAY));
                }

                int nextXp = LevelingHandler.getMaxXpForLevel(data.level());

                // Add Level Line
                if (data.level() >= maxLevel) {
                    lines.add(Text.literal("MAX LEVEL").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
                } else {
                    lines.add(Text.literal("Level " + data.level()).formatted(Formatting.GOLD));
                }

                // Add XP Line & Bar
                if (data.level() < maxLevel) {
                    lines.add(Text.literal("XP: " + data.xp() + " / " + nextXp).formatted(Formatting.GRAY));
                    lines.add(createProgressBar(data.xp(), nextXp));
                }

                // Extras
                if (data.prestige() > 0) {
                    lines.add(Text.literal("Prestige: " + data.prestige()).formatted(Formatting.RED));
                }
                if (data.level() >= config.soulboundLevel) {
                    lines.add(Text.literal("✦ Soulbound ✦").formatted(Formatting.AQUA, Formatting.BOLD));
                }

                // Glint Logic
                boolean shouldGlint = data.level() >= maxLevel;
                if (!stack.hasEnchantments()) {
                    stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, shouldGlint);
                }
            }
        });
    }

    private boolean isGoldTool(Item item) {
        return item == Items.GOLDEN_PICKAXE || item == Items.GOLDEN_AXE ||
                item == Items.GOLDEN_SHOVEL || item == Items.GOLDEN_HOE ||
                item == Items.GOLDEN_SWORD;
    }

    private boolean isLevelableTool(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof PickaxeItem || item instanceof AxeItem ||
                item instanceof ShovelItem || item instanceof SwordItem ||
                item instanceof HoeItem || isRanged(item);
    }

    private boolean isRanged(Item item) {
        return item instanceof BowItem || item instanceof CrossbowItem ||
                item instanceof TridentItem || item instanceof FishingRodItem;
    }

    private Text createProgressBar(int current, int max) {
        int totalBars = 20;
        if (max <= 0) max = 1;
        float percent = (float) current / max;
        if (percent > 1.0f) percent = 1.0f;

        int filledBars = (int) (percent * totalBars);
        int emptyBars = totalBars - filledBars;

        return Text.literal("[")
                .formatted(Formatting.WHITE)
                .append(Text.literal("|".repeat(Math.max(0, filledBars))).formatted(Formatting.DARK_GREEN))
                .append(Text.literal("|".repeat(Math.max(0, emptyBars))).formatted(Formatting.DARK_GRAY))
                .append(Text.literal("]").formatted(Formatting.WHITE));
    }

    private String getTitleForLevel(int level, int maxLevel) {
        if (level >= maxLevel) return "Legendary";
        if (level >= maxLevel * 0.8) return "Master";
        if (level >= maxLevel * 0.6) return "Expert";
        if (level >= maxLevel * 0.4) return "Adept";
        if (level >= maxLevel * 0.2) return "Apprentice";
        return "Novice";
    }
}