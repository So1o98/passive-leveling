package passive.leveling;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import passive.leveling.ModConfig;

public class LootHandler implements ServerTickEvents.EndTick {
    @Override
    public void onEndTick(MinecraftServer server) {
        // Placeholder
    }

    public static int getRequiredLevelForDrop(Item item) {
        if (PassiveLeveling.CONFIG == null) return 0;
        ModConfig.Leveling config = PassiveLeveling.CONFIG.leveling;

        // UPDATED: Using new milestone object names
        if (item == Items.STONE_PICKAXE) return config.pickaxe_milestones.wood_to_stone;
        if (item == Items.IRON_PICKAXE) return config.pickaxe_milestones.stone_to_iron;
        if (item == Items.DIAMOND_PICKAXE) return config.pickaxe_milestones.iron_to_diamond;
        if (item == Items.NETHERITE_PICKAXE) return config.pickaxe_milestones.diamond_to_netherite;

        if (item == Items.STONE_SWORD) return config.sword_milestones.wood_to_stone;
        if (item == Items.IRON_SWORD) return config.sword_milestones.stone_to_iron;
        if (item == Items.DIAMOND_SWORD) return config.sword_milestones.iron_to_diamond;
        if (item == Items.NETHERITE_SWORD) return config.sword_milestones.diamond_to_netherite;

        if (item == Items.STONE_AXE) return config.axe_milestones.wood_to_stone;
        if (item == Items.IRON_AXE) return config.axe_milestones.stone_to_iron;
        if (item == Items.DIAMOND_AXE) return config.axe_milestones.iron_to_diamond;
        if (item == Items.NETHERITE_AXE) return config.axe_milestones.diamond_to_netherite;

        if (item == Items.STONE_SHOVEL) return config.shovel_milestones.wood_to_stone;
        if (item == Items.IRON_SHOVEL) return config.shovel_milestones.stone_to_iron;
        if (item == Items.DIAMOND_SHOVEL) return config.shovel_milestones.iron_to_diamond;
        if (item == Items.NETHERITE_SHOVEL) return config.shovel_milestones.diamond_to_netherite;

        if (item == Items.STONE_HOE) return config.hoe_milestones.wood_to_stone;
        if (item == Items.IRON_HOE) return config.hoe_milestones.stone_to_iron;
        if (item == Items.DIAMOND_HOE) return config.hoe_milestones.iron_to_diamond;
        if (item == Items.NETHERITE_HOE) return config.hoe_milestones.diamond_to_netherite;

        return 0;
    }
}