package passive.leveling;

import net.minecraft.block.BlockState;
import net.minecraft.item.*;
import net.minecraft.registry.tag.BlockTags;

public class MiningValidator {
    public static boolean isValidMining(ItemStack stack, BlockState state) {
        Item item = stack.getItem();

        if (item instanceof PickaxeItem) {
            return state.isIn(BlockTags.PICKAXE_MINEABLE);
        }
        if (item instanceof AxeItem) {
            return state.isIn(BlockTags.AXE_MINEABLE);
        }
        if (item instanceof ShovelItem) {
            return state.isIn(BlockTags.SHOVEL_MINEABLE);
        }
        if (item instanceof HoeItem) {
            return state.isIn(BlockTags.HOE_MINEABLE);
        }
        return false;
    }
}