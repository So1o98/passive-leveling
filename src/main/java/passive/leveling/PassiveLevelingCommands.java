package passive.leveling;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PassiveLevelingCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("passiveleveling")
                .requires(source -> source.hasPermissionLevel(2)) // Requires OP Level 2

                // 1. Set XP
                .then(CommandManager.literal("setxp")
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                                .executes(PassiveLevelingCommands::executeSetXp)))

                // 2. Set Level
                .then(CommandManager.literal("setlevel")
                        .then(CommandManager.argument("level", IntegerArgumentType.integer(0))
                                .executes(PassiveLevelingCommands::executeSetLevel)))

                // 3. Set Prestige
                .then(CommandManager.literal("setprestige")
                        .then(CommandManager.argument("prestige", IntegerArgumentType.integer(0))
                                .executes(PassiveLevelingCommands::executeSetPrestige)))
        );
    }

    private static int executeSetXp(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            ItemStack stack = player.getMainHandStack();
            int amount = IntegerArgumentType.getInteger(context, "amount");
            LevelingData currentData = getOrInitData(stack);

            stack.set(PassiveLeveling.LEVEL_DATA, new LevelingData(currentData.level(), amount, currentData.prestige()));
            player.getInventory().markDirty();
            context.getSource().sendFeedback(() -> Text.literal("Set Tool XP to " + amount).formatted(Formatting.GREEN), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeSetLevel(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            ItemStack stack = player.getMainHandStack();
            int level = IntegerArgumentType.getInteger(context, "level");
            LevelingData currentData = getOrInitData(stack);

            stack.set(PassiveLeveling.LEVEL_DATA, new LevelingData(level, currentData.xp(), currentData.prestige()));
            player.getInventory().markDirty();
            context.getSource().sendFeedback(() -> Text.literal("Set Tool Level to " + level).formatted(Formatting.GREEN), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeSetPrestige(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            ItemStack stack = player.getMainHandStack();
            int prestige = IntegerArgumentType.getInteger(context, "prestige");
            LevelingData currentData = getOrInitData(stack);

            stack.set(PassiveLeveling.LEVEL_DATA, new LevelingData(currentData.level(), currentData.xp(), prestige));
            player.getInventory().markDirty();
            context.getSource().sendFeedback(() -> Text.literal("Set Tool Prestige to " + prestige).formatted(Formatting.GREEN), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    // Helper to safely get data or create defaults to prevent crashes on new items
    private static LevelingData getOrInitData(ItemStack stack) {
        if (stack.contains(PassiveLeveling.LEVEL_DATA)) {
            return stack.get(PassiveLeveling.LEVEL_DATA);
        } else {
            int startLevel = LevelingHandler.getStartingLevel(stack.getItem());
            return new LevelingData(startLevel, 0, 0);
        }
    }
}