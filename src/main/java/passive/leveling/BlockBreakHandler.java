package passive.leveling;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockBreakHandler implements PlayerBlockBreakEvents.After {

    @Override
    public void afterBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (world.isClient) return;
        if (PassiveLeveling.CONFIG == null) return;

        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty()) return;

        ModConfig.Rewards rewards = PassiveLeveling.CONFIG.rewards;
        int xpToGive = 0;
        Block block = state.getBlock();

        // 1. HIGH PRIORITY: SPECIFIC ORES (Override generic checks)
        if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) xpToGive = rewards.coal_xp;
        else if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) xpToGive = rewards.copper_xp;
        else if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) xpToGive = rewards.iron_xp;
        else if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE) xpToGive = rewards.gold_xp;
        else if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) xpToGive = rewards.lapis_xp;
        else if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) xpToGive = rewards.redstone_xp;
        else if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) xpToGive = rewards.diamond_xp;
        else if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) xpToGive = rewards.emerald_xp;
        else if (block == Blocks.NETHER_QUARTZ_ORE) xpToGive = rewards.quartz_xp;
        else if (block == Blocks.ANCIENT_DEBRIS) xpToGive = rewards.emerald_xp;

            // 2. SPECIFIC FARMING BLOCKS
        else if (block == Blocks.WHEAT || block == Blocks.CARROTS || block == Blocks.POTATOES || block == Blocks.BEETROOTS) {
            if (state.contains(net.minecraft.block.CropBlock.AGE) && state.get(net.minecraft.block.CropBlock.AGE) == 7) {
                xpToGive = rewards.harvest_xp;
            }
        }
        else if (block == Blocks.NETHER_WART) {
            if (state.contains(net.minecraft.block.NetherWartBlock.AGE) && state.get(net.minecraft.block.NetherWartBlock.AGE) == 3) {
                xpToGive = rewards.harvest_xp;
            }
        }
        else if (block == Blocks.MELON || block == Blocks.PUMPKIN) {
            xpToGive = rewards.harvest_xp;
        }

        // 3. GENERIC CATCH-ALL CHECKS (This covers everything else automatically!)

        // Logs (Overrides generic Axe check to give specific wood XP if you want it different, usually same as axe)
        else if (state.isIn(BlockTags.LOGS)) {
            xpToGive = rewards.wood_xp;
        }
        // Generic Pickaxe blocks (Terracotta, Stone, Bricks, Concrete, etc.)
        else if (state.isIn(BlockTags.PICKAXE_MINEABLE)) {
            xpToGive = rewards.stone_xp;
        }
        // Generic Shovel blocks (Dirt, Sand, Gravel, Mud, etc.)
        else if (state.isIn(BlockTags.SHOVEL_MINEABLE)) {
            xpToGive = rewards.digging_xp;
        }
        // Generic Axe blocks (Planks, Fences, Chests, etc.)
        else if (state.isIn(BlockTags.AXE_MINEABLE)) {
            xpToGive = rewards.wood_xp;
        }
        // Generic Hoe blocks (Leaves, Sculk, Hay, etc.)
        else if (state.isIn(BlockTags.HOE_MINEABLE)) {
            xpToGive = rewards.harvest_xp;
        }

        // 4. GIVE XP
        if (xpToGive > 0) {
            new LevelingHandler().addXp(world, player, stack, xpToGive);
        }
    }
}