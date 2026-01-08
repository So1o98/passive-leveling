package passive.leveling;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.Random;

public class GoldAbsorptionHandler {

    private static final Random RANDOM = new Random();

    public static void register() {
        UseItemCallback.EVENT.register(GoldAbsorptionHandler::handle);
    }

    private static TypedActionResult<ItemStack> handle(PlayerEntity player, World world, Hand hand) {
        if (world.isClient) return TypedActionResult.pass(ItemStack.EMPTY);

        ItemStack heldStack = player.getStackInHand(hand);

        // --- NEW FEATURE: Consume Gold Tool for Player XP ---
        // Condition: Holding a Gold Tool + Sneaking
        if (player.isSneaking() && isGoldTool(heldStack.getItem())) {

            // Calculate XP (Using same config values)
            int min = 100;
            int max = 300;
            if (PassiveLeveling.CONFIG != null) {
                min = PassiveLeveling.CONFIG.rewards.gold_absorb_min_xp;
                max = PassiveLeveling.CONFIG.rewards.gold_absorb_max_xp;
            }
            int xpToGive = RANDOM.nextInt(max - min + 1) + min;

            // Give Vanilla XP to Player
            player.addExperience(xpToGive);

            // Effects
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
            player.sendMessage(Text.literal("Consumed Gold Tool: +" + xpToGive + " Player XP").formatted(Formatting.YELLOW), true);

            // Destroy the item
            heldStack.decrement(1);

            return TypedActionResult.success(heldStack);
        }
        // ---------------------------------------------------

        // --- EXISTING FEATURE: Absorb Gold Tools into Leveling Tool ---
        // Condition: Holding a Leveling Tool (Not Sneaking, or Sneaking logic handled above)
        if (LevelingHandler.isLevelable(heldStack.getItem())) {

            // 1. Scan inventory for Gold Tools
            int totalXp = 0;
            int absorbedCount = 0;

            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack invStack = player.getInventory().getStack(i);

                if (isGoldTool(invStack.getItem())) {
                    // Calculate XP per item
                    int min = 100;
                    int max = 300;
                    if (PassiveLeveling.CONFIG != null) {
                        min = PassiveLeveling.CONFIG.rewards.gold_absorb_min_xp;
                        max = PassiveLeveling.CONFIG.rewards.gold_absorb_max_xp;
                    }

                    int xp = RANDOM.nextInt(max - min + 1) + min;
                    totalXp += xp;
                    absorbedCount++;

                    // Remove the gold tool
                    invStack.decrement(1);
                }
            }

            // 2. Apply XP if we found any
            if (absorbedCount > 0) {
                new LevelingHandler().addXp(world, player, heldStack, totalXp);

                world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);
                player.sendMessage(Text.literal("Absorbed " + absorbedCount + " Gold Tools for " + totalXp + " XP!").formatted(Formatting.GOLD), true);

                return TypedActionResult.success(heldStack);
            }
        }

        return TypedActionResult.pass(ItemStack.EMPTY);
    }

    private static boolean isGoldTool(Item item) {
        return item == Items.GOLDEN_PICKAXE || item == Items.GOLDEN_AXE ||
                item == Items.GOLDEN_SHOVEL || item == Items.GOLDEN_SWORD ||
                item == Items.GOLDEN_HOE;
    }
}