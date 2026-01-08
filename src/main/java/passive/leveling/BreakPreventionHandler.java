package passive.leveling;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BreakPreventionHandler implements PlayerBlockBreakEvents.Before {

    @Override
    public boolean beforeBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (world.isClient) return true;

        ItemStack stack = player.getMainHandStack();
        Item item = stack.getItem();

        // 1. CHECK GOLD BAN (Updated Message)
        if (PassiveLeveling.CONFIG != null && PassiveLeveling.CONFIG.restrictions.ban_gold_tools) {
            if (item == Items.GOLDEN_PICKAXE || item == Items.GOLDEN_AXE ||
                    item == Items.GOLDEN_SHOVEL || item == Items.GOLDEN_HOE ||
                    item == Items.GOLDEN_SWORD) {

                player.sendMessage(
                        Text.literal("Gold tools are too soft for mining! Absorb them instead.")
                                .formatted(Formatting.GOLD),
                        true
                );
                return false; // Cancel the break
            }
        }

        // 2. CHECK BROKEN TOOLS (Kept exactly as it was)
        if (LevelingHandler.isLevelable(item)) {
            boolean isTool = LevelingHandler.isPickaxe(item) ||
                    LevelingHandler.isAxe(item) ||
                    LevelingHandler.isShovel(item) ||
                    LevelingHandler.isHoe(item) ||
                    item == Items.BOW ||
                    item == Items.FISHING_ROD;

            if (isTool) {
                if (stack.getDamage() >= stack.getMaxDamage() - 1) {
                    player.sendMessage(Text.literal("❌ Tool is broken! Please repair it.").formatted(Formatting.RED), true);
                    return false;
                }
            }
        }

        return true;
    }
}