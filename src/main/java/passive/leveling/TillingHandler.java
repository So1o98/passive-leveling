package passive.leveling;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.HoeItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;

public class TillingHandler implements UseBlockCallback {
    @Override
    public ActionResult interact(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        if (world.isClient) return ActionResult.PASS;
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;

        ItemStack stack = player.getMainHandStack();
        if (!(stack.getItem() instanceof HoeItem)) return ActionResult.PASS;

        if (world.getBlockState(hitResult.getBlockPos()).getBlock() == Blocks.GRASS_BLOCK ||
                world.getBlockState(hitResult.getBlockPos()).getBlock() == Blocks.DIRT ||
                world.getBlockState(hitResult.getBlockPos()).getBlock() == Blocks.DIRT_PATH) {

            // Allow the till to happen, but give XP
            // Using a slight delay or assuming success is tricky with UseBlockCallback,
            // but for simplicity we grant XP if it's a valid tillable block.

            if (PassiveLeveling.CONFIG != null) {
                // UPDATED: tilling -> tilling_xp
                int xpAmount = PassiveLeveling.CONFIG.rewards.tilling_xp;
                new LevelingHandler().addXp(world, player, stack, xpAmount);
            }
        }

        return ActionResult.PASS;
    }
}