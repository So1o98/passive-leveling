package passive.leveling;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import java.util.Map;

public class SmithingPrestigeHandler implements UseBlockCallback {

    @Override
    public ActionResult interact(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        if (world.isClient) return ActionResult.PASS;
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
        if (!player.isSneaking()) return ActionResult.PASS;

        if (world.getBlockState(hitResult.getBlockPos()).getBlock() != Blocks.SMITHING_TABLE) {
            return ActionResult.PASS;
        }

        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty()) return ActionResult.PASS;
        if (!LevelingHandler.isLevelable(stack.getItem())) return ActionResult.PASS;

        if (PassiveLeveling.CONFIG == null) return ActionResult.PASS;

        // --- UPDATED VARIABLE NAME ---
        int maxLevel = PassiveLeveling.CONFIG.leveling.prestige_level_requirement;

        NbtCompound nbt = stack.getOrCreateNbt();
        int currentLevel = nbt.getInt("passive_level");

        if (currentLevel >= maxLevel) {
            // ... (Rest of logic is the same) ...
            int currentPrestige = nbt.getInt("passive_prestige");
            int newPrestige = currentPrestige + 1;

            Item resetItem = getWoodVersion(stack.getItem());

            ItemStack newStack = new ItemStack(resetItem);
            NbtCompound newNbt = newStack.getOrCreateNbt();

            newNbt.putInt("passive_level", 1);
            newNbt.putInt("passive_xp", 0);
            newNbt.putInt("passive_prestige", newPrestige);

            Map<Enchantment, Integer> enchants = EnchantmentHelper.get(stack);
            EnchantmentHelper.set(enchants, newStack);

            player.setStackInHand(Hand.MAIN_HAND, newStack);

            LevelingHandler.applyRandomEnchantment(player, newStack);

            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_SMITHING_TABLE_USE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            world.playSound(null, player.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.0f);

            player.sendMessage(Text.literal("✦ PRESTIGE SUCCESS! ✦").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), true);
            player.sendMessage(Text.literal("Your tool has been reforged. Current Prestige: " + newPrestige).formatted(Formatting.GRAY), false);

            if (world.getServer() != null) {
                world.getServer().getPlayerManager().broadcast(
                        Text.literal(player.getName().getString() + " has reached Prestige " + newPrestige + " on their tool!").formatted(Formatting.LIGHT_PURPLE),
                        false
                );
            }

            return ActionResult.SUCCESS;
        } else {
            player.sendMessage(Text.literal("Prestige available at Level " + maxLevel + " (Current: " + currentLevel + ")").formatted(Formatting.RED), true);
        }

        return ActionResult.PASS;
    }

    private Item getWoodVersion(Item item) {
        if (LevelingHandler.isPickaxe(item)) return Items.WOODEN_PICKAXE;
        if (LevelingHandler.isAxe(item)) return Items.WOODEN_AXE;
        if (LevelingHandler.isShovel(item)) return Items.WOODEN_SHOVEL;
        if (LevelingHandler.isHoe(item)) return Items.WOODEN_HOE;
        if (LevelingHandler.isSword(item)) return Items.WOODEN_SWORD;
        return item;
    }
}