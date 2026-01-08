package passive.leveling;

import net.minecraft.item.ItemStack;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SoulboundStorage {
    // Stores a list of items mapped to a player's UUID
    public static final Map<UUID, List<ItemStack>> SAVED_ITEMS = new HashMap<>();
}