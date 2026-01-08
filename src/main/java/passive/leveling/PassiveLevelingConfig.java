package passive.leveling;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "passiveleveling")
public class PassiveLevelingConfig implements ConfigData {

    // --- TAB 1: LEVELING SETTINGS ---

    @ConfigEntry.Category("leveling_settings")
    @ConfigEntry.Gui.Tooltip
    public int baseXpCost = 10;

    @ConfigEntry.Category("leveling_settings")
    @ConfigEntry.Gui.Tooltip
    public double xpExponentialMultiplier = 1.2;

    @ConfigEntry.Category("leveling_settings")
    @ConfigEntry.Gui.Tooltip
    public int xpLinearIncrease = 0;

    @ConfigEntry.Category("leveling_settings")
    @ConfigEntry.Gui.Tooltip
    public boolean enableTierIncreaseEnchants = true;

    @ConfigEntry.Category("leveling_settings")
    @ConfigEntry.Gui.Tooltip
    public int rangedEnchantInterval = 20;

    // MOVED: Now in leveling_settings
    @ConfigEntry.Category("leveling_settings")
    @ConfigEntry.Gui.Tooltip
    public int fishingEnchantInterval = 15;

    @ConfigEntry.Category("leveling_settings")
    @ConfigEntry.Gui.Tooltip
    public int prestigeLevelRequirement = 150;

    @ConfigEntry.Category("leveling_settings")
    @ConfigEntry.Gui.Tooltip
    public int soulboundLevel = 10;

    @ConfigEntry.Category("leveling_settings")
    @ConfigEntry.Gui.Tooltip
    public boolean enableTitlePrefixes = true;

    // Collapsible Milestone Objects
    @ConfigEntry.Category("leveling_settings")
    @ConfigEntry.Gui.CollapsibleObject
    public ToolMilestones pickaxe = new ToolMilestones(10, 20, 50, 100);

    @ConfigEntry.Category("leveling_settings")
    @ConfigEntry.Gui.CollapsibleObject
    public ToolMilestones sword = new ToolMilestones(10, 20, 50, 100);

    @ConfigEntry.Category("leveling_settings")
    @ConfigEntry.Gui.CollapsibleObject
    public ToolMilestones axe = new ToolMilestones(10, 20, 50, 100);

    @ConfigEntry.Category("leveling_settings")
    @ConfigEntry.Gui.CollapsibleObject
    public ToolMilestones shovel = new ToolMilestones(10, 20, 50, 100);

    @ConfigEntry.Category("leveling_settings")
    @ConfigEntry.Gui.CollapsibleObject
    public ToolMilestones hoe = new ToolMilestones(10, 20, 50, 100);


    // --- TAB 2: XP REWARDS ---

    @ConfigEntry.Category("xp_rewards")
    public int stoneXp = 12;
    @ConfigEntry.Category("xp_rewards")
    public int coalXp = 13;
    @ConfigEntry.Category("xp_rewards")
    public int copperXp = 14;
    @ConfigEntry.Category("xp_rewards")
    public int ironXp = 15;
    @ConfigEntry.Category("xp_rewards")
    public int goldXp = 15;
    @ConfigEntry.Category("xp_rewards")
    public int lapisXp = 18;
    @ConfigEntry.Category("xp_rewards")
    public int redstoneXp = 18;
    @ConfigEntry.Category("xp_rewards")
    public int diamondXp = 25;
    @ConfigEntry.Category("xp_rewards")
    public int emeraldXp = 30;
    @ConfigEntry.Category("xp_rewards")
    public int quartzXp = 15;
    @ConfigEntry.Category("xp_rewards")
    public int woodXp = 12;

    @ConfigEntry.Category("xp_rewards")
    public int passiveMobXp = 15;
    @ConfigEntry.Category("xp_rewards")
    public int hostileMobXp = 30;
    @ConfigEntry.Category("xp_rewards")
    public int bossMobXp = 100;

    @ConfigEntry.Category("xp_rewards")
    public int fishingXp = 20;
    @ConfigEntry.Category("xp_rewards")
    public int projectileXp = 15;

    @ConfigEntry.Category("xp_rewards")
    public int minGoldAbsorptionXp = 100;
    @ConfigEntry.Category("xp_rewards")
    public int maxGoldAbsorptionXp = 300;


    // --- TAB 3: GAME RESTRICTIONS ---

    @ConfigEntry.Category("game_restrictions")
    @ConfigEntry.Gui.Tooltip
    public boolean lockRecipesToTier = true;


    // --- DATA STRUCTURES ---
    public static class ToolMilestones {
        public int stoneLevel = 10;
        public int ironLevel = 20;
        public int diamondLevel = 50;
        public int netheriteLevel = 100;

        public ToolMilestones(int stone, int iron, int diamond, int netherite) {
            this.stoneLevel = stone;
            this.ironLevel = iron;
            this.diamondLevel = diamond;
            this.netheriteLevel = netherite;
        }
        public ToolMilestones() {}
    }
}