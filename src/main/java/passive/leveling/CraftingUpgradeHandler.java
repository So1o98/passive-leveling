package passive.leveling;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import java.util.Map;

public class CraftingUpgradeHandler implements UseBlockCallback {

    @Override
    public ActionResult interact(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        if (world.isClient) return ActionResult.PASS;
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
        if (!player.isSneaking()) return ActionResult.PASS;

        if (world.getBlockState(hitResult.getBlockPos()).getBlock() != Blocks.CRAFTING_TABLE) {
            return ActionResult.PASS;
        }

        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty()) return ActionResult.PASS;

        int level = 0;
        if (stack.hasNbt() && stack.getNbt().contains("passive_level")) {
            level = stack.getNbt().getInt("passive_level");
        }

        Item nextItem = LevelingHandler.getNextEvolution(stack.getItem(), level);

        if (nextItem != null) {
            ItemStack newStack = new ItemStack(nextItem);

            if (stack.hasNbt()) newStack.setNbt(stack.getNbt().copy());
            Map<Enchantment, Integer> enchants = EnchantmentHelper.get(stack);
            EnchantmentHelper.set(enchants, newStack);

            player.setStackInHand(Hand.MAIN_HAND, newStack);

            // --- REWARD: Apply Enchantment on Tier Upgrade ---
            LevelingHandler.applyRandomEnchantment(player, newStack);
            // -------------------------------------------------

            TierUnlocker.onUnlock(player, nextItem);

            player.sendMessage(Text.literal("Your tool has evolved into " + nextItem.getName().getString() + "!").formatted(Formatting.AQUA, Formatting.BOLD), true);
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            world.playSound(null, player.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.0f);

            if (world.getServer() != null) {
                world.getServer().getPlayerManager().broadcast(
                        Text.literal(player.getName().getString() + " has upgraded their tool to " + nextItem.getName().getString() + "!").formatted(Formatting.YELLOW),
                        false
                );
            }

            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }
}