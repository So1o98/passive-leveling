package passive.leveling;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

public class ModCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("passivelevels")
                .requires(source -> source.hasPermissionLevel(2)) // Operator only

                // --- SET LEVEL ---
                // Usage: /passivelevels setlevel <target> <level>
                // Usage: /passivelevels setlevel <level> (Defaults to self)
                .then(CommandManager.literal("setlevel")
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                .then(CommandManager.argument("level", IntegerArgumentType.integer(1))
                                        .executes(ModCommands::setLevelOther)))
                        .then(CommandManager.argument("level", IntegerArgumentType.integer(1))
                                .executes(ModCommands::setLevelSelf)))

                // --- SET XP ---
                // Usage: /passivelevels setxp <target> <amount>
                .then(CommandManager.literal("setxp")
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(ModCommands::setXpOther)))
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                                .executes(ModCommands::setXpSelf)))

                // --- SET PRESTIGE ---
                // Usage: /passivelevels setprestige <target> <amount>
                .then(CommandManager.literal("setprestige")
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(ModCommands::setPrestigeOther)))
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                                .executes(ModCommands::setPrestigeSelf)))
        );
    }

    public static void register() {}

    // ==========================================
    //               COMMAND EXECUTORS
    // ==========================================

    // --- LEVEL ---
    private static int setLevelSelf(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            int level = IntegerArgumentType.getInteger(context, "level");
            return applyLevel(player, level, context.getSource());
        } catch (Exception e) { return 0; }
    }

    private static int setLevelOther(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "target");
            int level = IntegerArgumentType.getInteger(context, "level");
            return applyLevel(player, level, context.getSource());
        } catch (Exception e) { return 0; }
    }

    // --- XP ---
    private static int setXpSelf(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            int amount = IntegerArgumentType.getInteger(context, "amount");
            return applyXp(player, amount, context.getSource());
        } catch (Exception e) { return 0; }
    }

    private static int setXpOther(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "target");
            int amount = IntegerArgumentType.getInteger(context, "amount");
            return applyXp(player, amount, context.getSource());
        } catch (Exception e) { return 0; }
    }

    // --- PRESTIGE ---
    private static int setPrestigeSelf(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            int amount = IntegerArgumentType.getInteger(context, "amount");
            return applyPrestige(player, amount, context.getSource());
        } catch (Exception e) { return 0; }
    }

    private static int setPrestigeOther(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "target");
            int amount = IntegerArgumentType.getInteger(context, "amount");
            return applyPrestige(player, amount, context.getSource());
        } catch (Exception e) { return 0; }
    }


    // ==========================================
    //               LOGIC HELPERS
    // ==========================================

    private static int applyLevel(ServerPlayerEntity player, int level, ServerCommandSource source) {
        ItemStack stack = player.getMainHandStack();

        if (stack.isEmpty() || !LevelingHandler.isLevelable(stack.getItem())) {
            source.sendError(Text.literal(player.getName().getString() + " is not holding a levelable tool!"));
            return 0;
        }

        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putInt("passive_level", level);
        nbt.putInt("passive_xp", 0);

        // Auto-adjust Tier based on new level
        if (PassiveLeveling.CONFIG != null) {
            ModConfig.Leveling config = PassiveLeveling.CONFIG.leveling;
            Item correctItem = stack.getItem();
            Item current = stack.getItem();

            if (LevelingHandler.isPickaxe(current)) {
                if (level >= config.pickaxe_milestones.diamond_to_netherite) correctItem = Items.NETHERITE_PICKAXE;
                else if (level >= config.pickaxe_milestones.iron_to_diamond) correctItem = Items.DIAMOND_PICKAXE;
                else if (level >= config.pickaxe_milestones.stone_to_iron) correctItem = Items.IRON_PICKAXE;
                else if (level >= config.pickaxe_milestones.wood_to_stone) correctItem = Items.STONE_PICKAXE;
                else correctItem = Items.WOODEN_PICKAXE;
            }
            else if (LevelingHandler.isAxe(current)) {
                if (level >= config.axe_milestones.diamond_to_netherite) correctItem = Items.NETHERITE_AXE;
                else if (level >= config.axe_milestones.iron_to_diamond) correctItem = Items.DIAMOND_AXE;
                else if (level >= config.axe_milestones.stone_to_iron) correctItem = Items.IRON_AXE;
                else if (level >= config.axe_milestones.wood_to_stone) correctItem = Items.STONE_AXE;
                else correctItem = Items.WOODEN_AXE;
            }
            else if (LevelingHandler.isSword(current)) {
                if (level >= config.sword_milestones.diamond_to_netherite) correctItem = Items.NETHERITE_SWORD;
                else if (level >= config.sword_milestones.iron_to_diamond) correctItem = Items.DIAMOND_SWORD;
                else if (level >= config.sword_milestones.stone_to_iron) correctItem = Items.IRON_SWORD;
                else if (level >= config.sword_milestones.wood_to_stone) correctItem = Items.STONE_SWORD;
                else correctItem = Items.WOODEN_SWORD;
            }
            else if (LevelingHandler.isShovel(current)) {
                if (level >= config.shovel_milestones.diamond_to_netherite) correctItem = Items.NETHERITE_SHOVEL;
                else if (level >= config.shovel_milestones.iron_to_diamond) correctItem = Items.DIAMOND_SHOVEL;
                else if (level >= config.shovel_milestones.stone_to_iron) correctItem = Items.IRON_SHOVEL;
                else if (level >= config.shovel_milestones.wood_to_stone) correctItem = Items.STONE_SHOVEL;
                else correctItem = Items.WOODEN_SHOVEL;
            }
            else if (LevelingHandler.isHoe(current)) {
                if (level >= config.hoe_milestones.diamond_to_netherite) correctItem = Items.NETHERITE_HOE;
                else if (level >= config.hoe_milestones.iron_to_diamond) correctItem = Items.DIAMOND_HOE;
                else if (level >= config.hoe_milestones.stone_to_iron) correctItem = Items.IRON_HOE;
                else if (level >= config.hoe_milestones.wood_to_stone) correctItem = Items.STONE_HOE;
                else correctItem = Items.WOODEN_HOE;
            }

            if (correctItem != current) {
                ItemStack newStack = new ItemStack(correctItem);
                newStack.setNbt(nbt);
                player.setStackInHand(Hand.MAIN_HAND, newStack);
                TierUnlocker.onUnlock(player, correctItem);
            }
        }

        source.sendFeedback(() -> Text.literal("Set tool level for " + player.getName().getString() + " to " + level), true);
        return 1;
    }

    private static int applyXp(ServerPlayerEntity player, int amount, ServerCommandSource source) {
        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty() || !LevelingHandler.isLevelable(stack.getItem())) {
            source.sendError(Text.literal(player.getName().getString() + " is not holding a levelable tool!"));
            return 0;
        }
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putInt("passive_xp", amount);
        source.sendFeedback(() -> Text.literal("Set tool XP for " + player.getName().getString() + " to " + amount), true);
        return 1;
    }

    private static int applyPrestige(ServerPlayerEntity player, int amount, ServerCommandSource source) {
        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty() || !LevelingHandler.isLevelable(stack.getItem())) {
            source.sendError(Text.literal(player.getName().getString() + " is not holding a levelable tool!"));
            return 0;
        }
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putInt("passive_prestige", amount);
        source.sendFeedback(() -> Text.literal("Set tool Prestige for " + player.getName().getString() + " to " + amount), true);
        return 1;
    }
}