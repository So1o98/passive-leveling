package passive.leveling;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.world.GameRules;

import java.util.*;

public class SoulboundHandler {

    // A temporary map to hold items while the player is dead
    private static final Map<UUID, List<ItemStack>> SAVED_ITEMS = new HashMap<>();

    public static void register() {
        // 1. EVENT: Before the player dies...
        ServerPlayerEvents.ALLOW_DEATH.register((player, damageSource, damageAmount) -> {
            // If keepInventory is ON, we don't need to do anything
            if (player.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) return true;

            List<ItemStack> itemsToSave = new ArrayList<>();

            // Scan the inventory
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);

                // If it is Soulbound, steal it!
                if (isSoulbound(stack)) {
                    itemsToSave.add(stack.copy());

                    // Remove it from inventory so it doesn't drop on the ground
                    player.getInventory().setStack(i, ItemStack.EMPTY);
                }
            }

            // If we stole any items, save them for later
            if (!itemsToSave.isEmpty()) {
                SAVED_ITEMS.put(player.getUuid(), itemsToSave);
            }

            return true; // Allow the death to happen normally
        });

        // 2. EVENT: When the player respawns...
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if (alive) return; // Ignore dimension changes (End/Nether portals)

            UUID uuid = oldPlayer.getUuid();
            if (SAVED_ITEMS.containsKey(uuid)) {
                List<ItemStack> saved = SAVED_ITEMS.get(uuid);

                // Give the items back to the new body
                for (ItemStack stack : saved) {
                    newPlayer.getInventory().offerOrDrop(stack);
                }

                // Clear the saved data
                SAVED_ITEMS.remove(uuid);
            }
        });
    }

    // --- THE IMPORTANT CHECK ---
    public static boolean isSoulbound(ItemStack stack) {
        if (!LevelingHandler.isLevelable(stack.getItem())) return false;
        if (!stack.hasNbt()) return false;

        int level = stack.getNbt().getInt("passive_level");
        int prestige = stack.getNbt().getInt("passive_prestige");

        // 1. SAFETY: Prestige items are ALWAYS Soulbound
        // (Even if they are Level 1)
        if (prestige > 0) return true;

        // 2. Regular Level Check
        int req = 20;
        if (PassiveLeveling.CONFIG != null) {
            req = PassiveLeveling.CONFIG.leveling.soulbound_level;
        }

        return level >= req;
    }
}