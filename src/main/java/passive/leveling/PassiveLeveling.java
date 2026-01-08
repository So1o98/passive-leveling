package passive.leveling;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback; // <--- NEW IMPORT
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PassiveLeveling implements ModInitializer {
    public static final String MOD_ID = "passiveleveling";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ModConfig CONFIG;

    @Override
    public void onInitialize() {
        LOGGER.info("PassiveLeveling initializing...");

        ModConfig.register();
        CONFIG = ModConfig.get();

        // --- EVENTS ---
        PlayerBlockBreakEvents.AFTER.register(new BlockBreakHandler());
        PlayerBlockBreakEvents.BEFORE.register(new BreakPreventionHandler());

        CombatHandler combat = new CombatHandler();
        ServerLivingEntityEvents.AFTER_DEATH.register(combat);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(combat);

        AttackEntityCallback.EVENT.register(new SwordPreventionHandler());

        UseBlockCallback.EVENT.register(new TillingHandler());
        UseBlockCallback.EVENT.register(new CraftingUpgradeHandler());
        UseBlockCallback.EVENT.register(new SmithingPrestigeHandler());

        ServerTickEvents.END_SERVER_TICK.register(new LootHandler());
        ServerTickEvents.END_SERVER_TICK.register(new FishingStatsHandler());

        // --- REGISTRIES ---
        GoldAbsorptionHandler.register();
        SoulboundHandler.register();

        // --- COMMANDS (FIXED) ---
        // This hooks your command class into the server startup
        CommandRegistrationCallback.EVENT.register(ModCommands::register);
    }
}