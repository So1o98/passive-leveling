package passive.leveling;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FishingStatsHandler implements ServerTickEvents.EndTick {

    private final Map<UUID, Integer> lastFishCounts = new HashMap<>();

    @Override
    public void onEndTick(MinecraftServer server) {
        // Iterate over ServerPlayerEntity
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

            // --- FIXED LINE BELOW ---
            // We must ask for (Stats.CUSTOM, Stats.FISH_CAUGHT)
            int currentFishCount = player.getStatHandler().getStat(Stats.CUSTOM, Stats.FISH_CAUGHT);

            UUID id = player.getUuid();

            if (!lastFishCounts.containsKey(id)) {
                lastFishCounts.put(id, currentFishCount);
                continue;
            }

            int lastCount = lastFishCounts.get(id);
            if (currentFishCount > lastCount) {
                // Fish caught!
                ItemStack stack = player.getMainHandStack();
                if (stack.getItem() == Items.FISHING_ROD) {
                    if (PassiveLeveling.CONFIG != null) {
                        int xpPerFish = PassiveLeveling.CONFIG.rewards.fishing_xp;
                        new LevelingHandler().addXp(player.getWorld(), player, stack, xpPerFish);
                    }
                }
                lastFishCounts.put(id, currentFishCount);
            }
        }
    }
}