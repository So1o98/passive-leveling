package passive.leveling;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;

@Config(name = "passiveleveling")
public class ModConfig implements ConfigData {

    @ConfigEntry.Category("leveling")
    @ConfigEntry.Gui.TransitiveObject
    public Leveling leveling = new Leveling();

    @ConfigEntry.Category("rewards")
    @ConfigEntry.Gui.TransitiveObject
    public Rewards rewards = new Rewards();

    @ConfigEntry.Category("restrictions")
    @ConfigEntry.Gui.TransitiveObject
    public Restrictions restrictions = new Restrictions();

    public static class Leveling {
        @ConfigEntry.Gui.Tooltip
        public int base_xp_cost = 10;

        @ConfigEntry.Gui.Tooltip
        public float xp_cost_multiplier = 1.1f;

        @ConfigEntry.Gui.Tooltip
        public int flat_xp_increase = 10;

        @ConfigEntry.Gui.Tooltip
        public boolean enable_level_up_enchantments = true;

        @ConfigEntry.Gui.Tooltip
        public int bow_enchant_interval = 25;

        @ConfigEntry.Gui.Tooltip
        public int rod_enchant_interval = 10;

        @ConfigEntry.Gui.Tooltip
        public int prestige_level_requirement = 100;

        @ConfigEntry.Gui.Tooltip
        public int soulbound_level = 100;

        @ConfigEntry.Gui.Tooltip
        public boolean enable_title_prefixes = true;

        @ConfigEntry.Gui.Excluded
        public boolean require_smithing_for_evolution = true;

        @ConfigEntry.Gui.CollapsibleObject
        public ToolThresholds pickaxe_milestones = new ToolThresholds();
        @ConfigEntry.Gui.CollapsibleObject
        public ToolThresholds sword_milestones = new ToolThresholds();
        @ConfigEntry.Gui.CollapsibleObject
        public ToolThresholds axe_milestones = new ToolThresholds();
        @ConfigEntry.Gui.CollapsibleObject
        public ToolThresholds shovel_milestones = new ToolThresholds();
        @ConfigEntry.Gui.CollapsibleObject
        public ToolThresholds hoe_milestones = new ToolThresholds();
    }

    public static class ToolThresholds {
        public int wood_to_stone = 10;
        public int stone_to_iron = 20;
        public int iron_to_diamond = 50;
        public int diamond_to_netherite = 100;
    }

    public static class Rewards {
        // --- NUMISMATIC OVERHAUL SETTINGS (NEW) ---
        @ConfigEntry.Gui.Tooltip
        public int min_level_up_coins = 5; // Minimum bronze coins on level up
        @ConfigEntry.Gui.Tooltip
        public int max_level_up_coins = 20; // Maximum bronze coins on level up
        @ConfigEntry.Gui.Tooltip
        public int tier_upgrade_silver_coins = 1; // Silver coins on evolution

        // --- STANDARD XP REWARDS ---
        public int stone_xp = 10;
        public int coal_xp = 2;
        public int copper_xp = 3;
        public int iron_xp = 4;
        public int gold_xp = 5;
        public int lapis_xp = 6;
        public int redstone_xp = 6;
        public int diamond_xp = 10;
        public int emerald_xp = 15;
        public int quartz_xp = 3;
        public int wood_xp = 2;
        public int passive_mob_xp = 2;
        public int hostile_mob_xp = 10;
        public int boss_mob_xp = 100;
        public int fishing_xp = 15;
        public int projectile_xp = 5;
        public int gold_absorb_min_xp = 100;
        public int gold_absorb_max_xp = 500;

        @ConfigEntry.Gui.Excluded public int breeding_xp = 5;
        @ConfigEntry.Gui.Excluded public int digging_xp = 1;
        @ConfigEntry.Gui.Excluded public int tilling_xp = 1;
        @ConfigEntry.Gui.Excluded public int harvest_xp = 1;
    }

    public static class Restrictions {
        @ConfigEntry.Gui.Tooltip
        public boolean lock_recipes_to_tier = true;

        @ConfigEntry.Gui.Excluded public boolean allow_crafting_unlocks = true;
        @ConfigEntry.Gui.Excluded public boolean ban_gold_tools = true;
    }

    public static void register() {
        AutoConfig.register(ModConfig.class, JanksonConfigSerializer::new);
    }

    public static ModConfig get() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig();
    }
}