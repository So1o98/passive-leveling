package passive.leveling.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MinecraftServer.class)
public class ExampleMixin {
    // We removed the broken "Hello World" code.
    // This file is now safe and will not crash the game.
}