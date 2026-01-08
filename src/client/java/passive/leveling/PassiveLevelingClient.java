package passive.leveling;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.item.Item;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.google.common.base.Strings;

public class PassiveLevelingClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            if (LevelingHandler.isLevelable(stack.getItem())) {

                int level = 0;
                int xp = 0;
                int prestige = 0;

                if (stack.hasNbt()) {
                    level = stack.getNbt().getInt("passive_level");
                    xp = stack.getNbt().getInt("passive_xp");
                    prestige = stack.getNbt().getInt("passive_prestige");
                }

                if (level == 0) level = 1;
                int req = LevelingHandler.getRequiredXpForNextLevel(level);

                // 1. RANK
                if (PassiveLeveling.CONFIG != null && PassiveLeveling.CONFIG.leveling.enable_title_prefixes) {
                    String title = getTitle(level, prestige);
                    Text rankText = Text.literal("Rank: ").formatted(Formatting.GRAY)
                            .append(Text.literal(title).formatted(Formatting.AQUA));
                    lines.add(rankText);
                }

                // 2. LEVEL
                lines.add(Text.literal("Level " + level).formatted(Formatting.GOLD));

                // 3. XP TEXT
                lines.add(Text.literal("XP: " + xp + " / " + req).formatted(Formatting.GRAY));

                // 4. XP BAR
                lines.add(drawProgressBar(xp, req));

                // 5. SOULBOUND
                int soulboundReq = getSoulboundReq();
                if (level >= soulboundReq || prestige > 0) {
                    lines.add(Text.literal("✦ Soulbound ✦").formatted(Formatting.AQUA));
                }

                // 6. PRESTIGE
                if (prestige > 0) {
                    lines.add(Text.literal("Prestige: " + prestige).formatted(Formatting.LIGHT_PURPLE));
                }

                // 7. WARNINGS
                int prestigeReq = getPrestigeReq();
                if (level >= prestigeReq) {
                    lines.add(Text.literal("MAX LEVEL - Ready to Prestige!").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
                }
                else {
                    int maxLevel = getMaxLevelForTier(stack.getItem());
                    if (maxLevel != -1 && level >= maxLevel) {
                        lines.add(Text.literal("MAX LEVEL - Upgrade Required!").formatted(Formatting.RED, Formatting.BOLD));
                    }
                }
            }
        });
    }

    // --- CRASH FIX HERE ---
    private Text drawProgressBar(int current, int max) {
        int totalBars = 20;

        // Safety check to prevent divide by zero
        if (max <= 0) max = 1;

        // Calculate percent
        float percent = (float) current / max;

        // CLAMP: Ensure percent is never more than 100% (1.0) or less than 0%
        if (percent > 1.0f) percent = 1.0f;
        if (percent < 0.0f) percent = 0.0f;

        int filledBars = (int) (totalBars * percent);
        int emptyBars = totalBars - filledBars;

        String filled = Strings.repeat("|", filledBars);
        String empty = Strings.repeat("|", emptyBars);

        return Text.literal("[")
                .formatted(Formatting.WHITE)
                .append(Text.literal(filled).formatted(Formatting.DARK_GREEN))
                .append(Text.literal(empty).formatted(Formatting.DARK_GRAY))
                .append(Text.literal("]").formatted(Formatting.WHITE));
    }

    private String getTitle(int level, int prestige) {
        if (prestige > 0) return "Legendary";
        int prestigeReq = getPrestigeReq();
        if (level >= prestigeReq) return "Mythic";
        if (level >= 80) return "Grandmaster";
        if (level >= 60) return "Master";
        if (level >= 40) return "Expert";
        if (level >= 20) return "Adept";
        return "Novice";
    }

    private int getMaxLevelForTier(Item item) {
        if (PassiveLeveling.CONFIG == null) return -1;
        ModConfig.Leveling config = PassiveLeveling.CONFIG.leveling;
        int tier1 = 0, tier2 = 0, tier3 = 0, tier4 = 0;

        if (LevelingHandler.isPickaxe(item)) {
            tier1 = config.pickaxe_milestones.wood_to_stone;
            tier2 = config.pickaxe_milestones.stone_to_iron;
            tier3 = config.pickaxe_milestones.iron_to_diamond;
            tier4 = config.pickaxe_milestones.diamond_to_netherite;
        } else if (LevelingHandler.isSword(item)) {
            tier1 = config.sword_milestones.wood_to_stone;
            tier2 = config.sword_milestones.stone_to_iron;
            tier3 = config.sword_milestones.iron_to_diamond;
            tier4 = config.sword_milestones.diamond_to_netherite;
        } else if (LevelingHandler.isAxe(item)) {
            tier1 = config.axe_milestones.wood_to_stone;
            tier2 = config.axe_milestones.stone_to_iron;
            tier3 = config.axe_milestones.iron_to_diamond;
            tier4 = config.axe_milestones.diamond_to_netherite;
        } else if (LevelingHandler.isShovel(item)) {
            tier1 = config.shovel_milestones.wood_to_stone;
            tier2 = config.shovel_milestones.stone_to_iron;
            tier3 = config.shovel_milestones.iron_to_diamond;
            tier4 = config.shovel_milestones.diamond_to_netherite;
        } else if (LevelingHandler.isHoe(item)) {
            tier1 = config.hoe_milestones.wood_to_stone;
            tier2 = config.hoe_milestones.stone_to_iron;
            tier3 = config.hoe_milestones.iron_to_diamond;
            tier4 = config.hoe_milestones.diamond_to_netherite;
        } else {
            return -1;
        }

        String name = item.toString();
        if (name.contains("wooden")) return tier1;
        if (name.contains("stone")) return tier2;
        if (name.contains("iron")) return tier3;
        if (name.contains("diamond")) return tier4;

        return -1;
    }

    private int getPrestigeReq() {
        if (PassiveLeveling.CONFIG == null) return 100;
        try { return PassiveLeveling.CONFIG.leveling.prestige_level_requirement; } catch (Exception e) { return 100; }
    }

    private int getSoulboundReq() {
        if (PassiveLeveling.CONFIG == null) return 100;
        try { return PassiveLeveling.CONFIG.leveling.soulbound_level; } catch (Exception e) { return 100; }
    }
}