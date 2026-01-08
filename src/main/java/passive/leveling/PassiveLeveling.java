package passive.leveling;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.ComponentType;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;

import java.util.concurrent.ThreadLocalRandom;

public class PassiveLeveling implements ModInitializer {

    public static final String MOD_ID = "passiveleveling";

    public static final ComponentType<LevelingData> LEVEL_DATA = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(MOD_ID, "level_data"),
            ComponentType.<LevelingData>builder()
                    .codec(LevelingData.CODEC)
                    .packetCodec(LevelingData.PACKET_CODEC)
                    .build()
    );

    @Override
    public void onInitialize() {
        AutoConfig.register(PassiveLevelingConfig.class, GsonConfigSerializer::new);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            PassiveLevelingCommands.register(dispatcher, registryAccess, environment);
        });

        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (server.getTicks() % 20 == 0) {
                    RecipeUnlockHandler.checkAndUnlock(player);
                }
            }
        });

        // 1. MINING XP & TOOL VALIDATION
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient && !player.isCreative()) {
                ItemStack stack = player.getMainHandStack();

                // A. Initialize Data for Loot/Bonus Chest Tools
                if (isLevelableTool(stack) && !stack.contains(LEVEL_DATA)) {
                    int startingLevel = LevelingHandler.getStartingLevel(stack.getItem());
                    stack.set(LEVEL_DATA, new LevelingData(startingLevel, 0, 0));
                    // No return here, let it proceed to validation checks
                }

                // B. Disable Gold Tools for Mining
                if (isGoldTool(stack.getItem())) {
                    player.sendMessage(Text.literal("Gold tools are too soft for mining! Absorb them instead.").formatted(Formatting.GOLD), true);
                    return false;
                }

                // C. Check Broken State
                if (isBroken(stack)) {
                    player.sendMessage(Text.literal("⚠ Tool is broken! ⚠").formatted(Formatting.RED), true);
                    return false;
                }
            }
            return true;
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient && !player.isCreative()) {
                ItemStack stack = player.getMainHandStack();
                if (isLevelableTool(stack) && !isBroken(stack)) {
                    // Ensure data exists before adding XP (Safety check)
                    if (!stack.contains(LEVEL_DATA)) {
                        int startingLevel = LevelingHandler.getStartingLevel(stack.getItem());
                        stack.set(LEVEL_DATA, new LevelingData(startingLevel, 0, 0));
                    }

                    if (MiningValidator.isValidMining(stack, state)) {
                        int xp = LevelingHandler.getBlockXp(state);
                        LevelingHandler.addXp((ServerPlayerEntity) player, stack, xp);
                    }
                }
            }
        });

        // 2. COMBAT XP & TOOL VALIDATION
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient) {
                ItemStack stack = player.getStackInHand(hand);

                // Initialize Data for Loot Tools (Combat)
                if (isLevelableTool(stack) && !stack.contains(LEVEL_DATA)) {
                    int startingLevel = LevelingHandler.getStartingLevel(stack.getItem());
                    stack.set(LEVEL_DATA, new LevelingData(startingLevel, 0, 0));
                }

                if (isGoldTool(stack.getItem())) {
                    player.sendMessage(Text.literal("Gold tools are too soft for combat! Absorb them instead.").formatted(Formatting.GOLD), true);
                    return ActionResult.FAIL;
                }

                if (isBroken(stack)) {
                    player.sendMessage(Text.literal("⚠ Tool is broken! ⚠").formatted(Formatting.RED), true);
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        });

        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
            if (!world.isClient && entity instanceof ServerPlayerEntity player) {
                ItemStack stack = player.getMainHandStack();
                if (isLevelableTool(stack) && !isBroken(stack)) {
                    LevelingHandler.addXp(player, stack, LevelingHandler.getEntityXp(killedEntity));
                }
            }
        });

        // 3. GOLD ABSORPTION (Offhand Shift-Right-Click Only)
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient) return TypedActionResult.pass(player.getStackInHand(hand));

            if (hand == net.minecraft.util.Hand.OFF_HAND) {
                ItemStack offHandStack = player.getStackInHand(hand);
                if (isGoldTool(offHandStack.getItem())) {
                    ItemStack mainHandStack = player.getMainHandStack();
                    PassiveLevelingConfig config = LevelingHandler.getConfig();

                    int min = config.minGoldAbsorptionXp;
                    int max = config.maxGoldAbsorptionXp;
                    int xpAmount = ThreadLocalRandom.current().nextInt(min, max + 1);

                    // Case A: Holding a Tool -> Give Tool XP
                    if (isLevelableTool(mainHandStack)) {
                        // Ensure main hand tool has data before adding XP
                        if (!mainHandStack.contains(LEVEL_DATA)) {
                            int startingLevel = LevelingHandler.getStartingLevel(mainHandStack.getItem());
                            mainHandStack.set(LEVEL_DATA, new LevelingData(startingLevel, 0, 0));
                        }

                        player.sendMessage(Text.literal("Absorbed Gold! +" + xpAmount + " Tool XP").formatted(Formatting.GOLD), true);
                        LevelingHandler.addXp((ServerPlayerEntity) player, mainHandStack, xpAmount, false);

                        offHandStack.decrement(1);
                        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
                        return TypedActionResult.success(offHandStack);
                    }

                    // Case B: Empty Hand + Sneaking -> Give Player XP
                    if (mainHandStack.isEmpty() && player.isSneaking()) {
                        player.sendMessage(Text.literal("Absorbed Gold! +" + xpAmount + " Player XP").formatted(Formatting.YELLOW), true);
                        player.addExperience(xpAmount);

                        offHandStack.decrement(1);
                        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
                        return TypedActionResult.success(offHandStack);
                    }
                }
            }
            return TypedActionResult.pass(player.getStackInHand(hand));
        });

        // 4. BLOCK INTERACTION (Evolution & Prestige)
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient && player.isSneaking() && hand == net.minecraft.util.Hand.MAIN_HAND) {
                BlockState blockState = world.getBlockState(hitResult.getBlockPos());
                ItemStack stack = player.getMainHandStack();

                // Initialize Data check
                if (isLevelableTool(stack) && !stack.contains(LEVEL_DATA)) {
                    int startingLevel = LevelingHandler.getStartingLevel(stack.getItem());
                    stack.set(LEVEL_DATA, new LevelingData(startingLevel, 0, 0));
                }

                // A. SMITHING TABLE -> PRESTIGE
                if (blockState.isOf(Blocks.SMITHING_TABLE)) {
                    if (LevelingHandler.handlePrestigeInteraction((ServerPlayerEntity) player, stack)) {
                        return ActionResult.SUCCESS;
                    }
                }

                // B. CRAFTING TABLE -> EVOLUTION (Tier Upgrade)
                if (blockState.isOf(Blocks.CRAFTING_TABLE)) {
                    if (LevelingHandler.tryEvolve((ServerPlayerEntity) player, stack)) {
                        return ActionResult.SUCCESS;
                    }
                }
            }
            return ActionResult.PASS;
        });

        // 5. SOULBOUND RESTORE
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) SoulboundManager.restoreItems(oldPlayer, newPlayer);
        });
    }

    private boolean isGoldTool(Item item) {
        return item == Items.GOLDEN_PICKAXE || item == Items.GOLDEN_AXE ||
                item == Items.GOLDEN_SHOVEL || item == Items.GOLDEN_HOE ||
                item == Items.GOLDEN_SWORD;
    }

    private boolean isLevelableTool(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof PickaxeItem || item instanceof AxeItem ||
                item instanceof ShovelItem || item instanceof SwordItem ||
                item instanceof HoeItem || item instanceof BowItem ||
                item instanceof CrossbowItem || item instanceof FishingRodItem ||
                item instanceof TridentItem;
    }

    private static boolean isBroken(ItemStack stack) {
        return stack.isDamageable() && stack.getDamage() >= stack.getMaxDamage() - 1;
    }
}