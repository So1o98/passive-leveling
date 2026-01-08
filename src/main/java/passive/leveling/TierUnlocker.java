package passive.leveling;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.Set;

public class TierUnlocker {

    // Check if the player is allowed to craft this specific item
    public static boolean canCraft(PlayerEntity player, Item item) {
        if (player.isCreative() || player.isSpectator()) return true;

        String id = item.toString(); // e.g., "diamond_pickaxe"

        // 1. Always allow basic items (Wood, Gold, Bows) and non-tools
        if (id.contains("wooden") || id.contains("gold")) return true;
        if (!isTool(id)) return true;

        // 2. CHECK CONFIG
        if (PassiveLeveling.CONFIG != null && !PassiveLeveling.CONFIG.restrictions.allow_crafting_unlocks) {
            return false;
        }

        // 3. GENERATE THE REQUIRED TAG
        // Example: "passive_unlock_diamond_pickaxe"
        String requiredTag = getUnlockTag(id);

        // If we couldn't figure out the tag (weird modded item?), default to allowed
        if (requiredTag == null) return true;

        // 4. CHECK IF PLAYER HAS IT
        return player.getCommandTags().contains(requiredTag);
    }

    // Call this when a tool evolves
    public static void onUnlock(PlayerEntity player, Item newItem) {
        if (PassiveLeveling.CONFIG != null && !PassiveLeveling.CONFIG.restrictions.allow_crafting_unlocks) {
            return;
        }

        String id = newItem.toString();
        String unlockTag = getUnlockTag(id);

        if (unlockTag != null) {
            // Only add tag if they don't have it yet
            if (!player.getCommandTags().contains(unlockTag)) {
                player.addCommandTag(unlockTag);

                // Formatting the message to look nice (e.g. "Diamond Pickaxe")
                String prettyName = newItem.getName().getString();
                player.sendMessage(Text.literal("🔓 RECIPE UNLOCKED: " + prettyName + "!").formatted(Formatting.GREEN, Formatting.BOLD), false);
            }
        }
    }

    // --- HELPER METHODS ---

    // Generates the specific tag key, e.g., "passive_unlock_iron_shovel"
    private static String getUnlockTag(String id) {
        String material = "";
        String type = "";

        // Determine Material
        if (id.contains("stone")) material = "stone";
        else if (id.contains("iron")) material = "iron";
        else if (id.contains("diamond")) material = "diamond";
        else if (id.contains("netherite")) material = "netherite";
        else return null; // Not a restricted material

        // Determine Type
        if (id.contains("_pickaxe")) type = "pickaxe";
        else if (id.contains("_axe")) type = "axe"; // Covered by pickaxe check if not careful, but "_" helps
        else if (id.contains("_shovel")) type = "shovel";
        else if (id.contains("_hoe")) type = "hoe";
        else if (id.contains("_sword")) type = "sword";
        else return null; // Not a standard tool

        return "passive_unlock_" + material + "_" + type;
    }

    private static boolean isTool(String id) {
        return id.contains("_pickaxe") || id.contains("_axe") || id.contains("_shovel") || id.contains("_hoe") || id.contains("_sword");
    }
}