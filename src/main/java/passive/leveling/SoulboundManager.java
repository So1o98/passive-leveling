package passive.leveling;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameRules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SoulboundManager {

    private static final Map<UUID, List<ItemStack>> SAVED_ITEMS = new HashMap<>();

    public static void saveItems(ServerPlayerEntity player) {
        if (player.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) return;

        List<ItemStack> soulboundLoot = new ArrayList<>();
        PassiveLevelingConfig config = AutoConfig.getConfigHolder(PassiveLevelingConfig.class).getConfig();

        // Scan Main Inventory
        for (int i = 0; i < player.getInventory().main.size(); i++) {
            ItemStack stack = player.getInventory().main.get(i);
            if (isSoulbound(stack, config)) {
                soulboundLoot.add(stack.copy());
                player.getInventory().main.set(i, ItemStack.EMPTY);
            }
        }

        // Scan Armor
        for (int i = 0; i < player.getInventory().armor.size(); i++) {
            ItemStack stack = player.getInventory().armor.get(i);
            if (isSoulbound(stack, config)) {
                soulboundLoot.add(stack.copy());
                player.getInventory().armor.set(i, ItemStack.EMPTY);
            }
        }

        // Scan Offhand
        for (int i = 0; i < player.getInventory().offHand.size(); i++) {
            ItemStack stack = player.getInventory().offHand.get(i);
            if (isSoulbound(stack, config)) {
                soulboundLoot.add(stack.copy());
                player.getInventory().offHand.set(i, ItemStack.EMPTY);
            }
        }

        if (!soulboundLoot.isEmpty()) {
            SAVED_ITEMS.put(player.getUuid(), soulboundLoot);
        }
    }

    public static void restoreItems(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer) {
        UUID id = oldPlayer.getUuid();

        if (SAVED_ITEMS.containsKey(id)) {
            List<ItemStack> saved = SAVED_ITEMS.get(id);

            for (ItemStack stack : saved) {
                if (!newPlayer.getInventory().insertStack(stack)) {
                    newPlayer.dropItem(stack, false);
                }
            }
            SAVED_ITEMS.remove(id);
        }
    }

    private static boolean isSoulbound(ItemStack stack, PassiveLevelingConfig config) {
        if (stack.isEmpty()) return false;
        LevelingData data = stack.getOrDefault(PassiveLeveling.LEVEL_DATA, LevelingData.DEFAULT);
        return data.level() >= config.soulboundLevel;
    }
}