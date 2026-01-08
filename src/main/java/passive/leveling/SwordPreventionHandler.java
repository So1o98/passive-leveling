package passive.leveling;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

public class SwordPreventionHandler implements AttackEntityCallback {

    @Override
    public ActionResult interact(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        if (world.isClient || hand != Hand.MAIN_HAND) return ActionResult.PASS;

        ItemStack stack = player.getMainHandStack();
        Item item = stack.getItem();

        // 1. CHECK GOLD BAN (New Feature)
        if (PassiveLeveling.CONFIG != null && PassiveLeveling.CONFIG.restrictions.ban_gold_tools) {
            if (isGoldTool(item)) {
                player.sendMessage(
                        Text.literal("Gold tools are too soft for combat! Absorb them instead.")
                                .formatted(Formatting.GOLD),
                        true
                );
                return ActionResult.FAIL;
            }
        }

        // 2. CHECK BROKEN WEAPONS (Existing Logic)
        // Check Swords AND Axes
        if (isSword(item) || LevelingHandler.isAxe(item)) {

            if (stack.isDamaged() && stack.getDamage() >= stack.getMaxDamage() - 1) {
                player.sendMessage(Text.literal("❌ Weapon is broken! Please repair it.").formatted(Formatting.RED), true);
                return ActionResult.FAIL;
            }
        }

        return ActionResult.PASS;
    }

    // Helper to identify gold tools
    private boolean isGoldTool(Item item) {
        return item == Items.GOLDEN_SWORD || item == Items.GOLDEN_AXE ||
                item == Items.GOLDEN_PICKAXE || item == Items.GOLDEN_SHOVEL ||
                item == Items.GOLDEN_HOE;
    }

    private boolean isSword(Item item) {
        return item == Items.WOODEN_SWORD || item == Items.STONE_SWORD ||
                item == Items.IRON_SWORD || item == Items.DIAMOND_SWORD ||
                item == Items.NETHERITE_SWORD;
    }
}