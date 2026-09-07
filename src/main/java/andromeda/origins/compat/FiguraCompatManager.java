package andromeda.origins.compat;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.command.CommandOutput;

/** Keeps legacy Figura boolean resources from getting stuck across respawns. */
public final class FiguraCompatManager {
    private static final String[] LEGACY_RESOURCES = {
        "andromeda_origins:common/figura_1",
        "andromeda_origins:common/figura_2",
        "andromeda_origins:common/figura_3",
        "andromeda_origins:common/figura_4",
        "andromeda_origins:common/figura_5",
        "andromeda_origins:humanity/figura_mortal_resolve"
    };

    private FiguraCompatManager() {}

    public static void register() {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> resetLegacyHooks(newPlayer));
    }

    public static void resetLegacyHooks(ServerPlayerEntity player) {
        for (String resource : LEGACY_RESOURCES) {
            setResource(player, resource, 0);
        }
    }

    public static void setResource(ServerPlayerEntity player, String resource, int value) {
        ServerCommandSource silentSource = player.getCommandSource().withLevel(4).withOutput(CommandOutput.DUMMY);
        player.getServerWorld().getServer().getCommandManager().executeWithPrefix(
            silentSource, "resource set @s " + resource + " " + value
        );
    }
}
