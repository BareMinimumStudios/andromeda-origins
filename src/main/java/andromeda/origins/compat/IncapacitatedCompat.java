package andromeda.origins.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Optional compatibility bridge for CartoonishVillain's Incapacitated.
 *
 * This module intentionally uses reflection instead of a hard compile/runtime dependency. That
 * keeps Andromeda Origins usable without Incapacitated while still allowing us to ask the mod's
 * own player-data object whether the player is actually downed before running its revive command.
 */
public final class IncapacitatedCompat {

    private static final Logger LOGGER = LoggerFactory.getLogger("Andromeda Origins/Incapacitated Compat");
    private static final String MOD_ID = "incapacitated";
    private static boolean reflectionWarningLogged = false;

    private IncapacitatedCompat() {}

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    /**
     * @return true only when Incapacitated is loaded and its own component reports the player downed.
     */
    public static boolean isDowned(ServerPlayerEntity player) {
        if (!isLoaded()) {
            return false;
        }

        try {
            Object platform = getPlatform();
            Object playerData = getPlayerData(platform, player);
            if (playerData == null) {
                return false;
            }

            Method isIncapacitated = playerData.getClass().getMethod("isIncapacitated");
            return Boolean.TRUE.equals(isIncapacitated.invoke(playerData));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (!reflectionWarningLogged) {
                reflectionWarningLogged = true;
                LOGGER.warn("Could not read Incapacitated player state. Safe revive will no-op rather than forcing a revive.", exception);
            }
            return false;
        }
    }

    private static Object getPlatform() throws ReflectiveOperationException {
        Class<?> servicesClass = Class.forName("com.cartoonishvillain.incapacitated.platform.Services");
        Field platformField = servicesClass.getField("PLATFORM");
        return platformField.get(null);
    }

    private static Object getPlayerData(Object platform, ServerPlayerEntity player) throws ReflectiveOperationException {
        Method getPlayerData = Arrays.stream(platform.getClass().getMethods())
            .filter(method -> method.getName().equals("getPlayerData") && method.getParameterCount() == 1)
            .findFirst()
            .orElseThrow(() -> new NoSuchMethodException("getPlayerData"));
        return getPlayerData.invoke(platform, player);
    }

    /**
     * Incapacitated keeps the last damage and pre-hit health in runtime component fields. They are
     * not cleared by its normal revive method. Resetting them after an Andromeda-triggered revive
     * prevents an interrupted/cancelled damage sequence from leaking stale overkill data into the
     * next hit.
     */
    private static void resetDamageTracking(ServerPlayerEntity player) {
        if (!isLoaded()) {
            return;
        }

        try {
            Object platform = getPlatform();
            Object playerData = getPlayerData(platform, player);
            if (playerData == null) {
                return;
            }

            playerData.getClass().getMethod("setLastDmgTaken", float.class).invoke(playerData, 0.0F);
            playerData.getClass().getMethod("setLastHealthBeforeDamage", float.class)
                .invoke(playerData, Math.max(1.0F, player.getHealth()));

            Method writePlayerData = Arrays.stream(platform.getClass().getMethods())
                .filter(method -> method.getName().equals("writePlayerData") && method.getParameterCount() == 2)
                .findFirst()
                .orElseThrow(() -> new NoSuchMethodException("writePlayerData"));
            writePlayerData.invoke(platform, player, playerData);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (!reflectionWarningLogged) {
                reflectionWarningLogged = true;
                LOGGER.warn("Could not reset Incapacitated transient damage tracking.", exception);
            }
        }
    }

    /**
     * Revives only if Incapacitated says the player is currently downed. This avoids the upstream
     * setDowned false command resetting a healthy player's health to the configured revive health.
     */
    public static boolean safeRevive(ServerPlayerEntity player) {
        if (!isDowned(player)) {
            return false;
        }

        ServerCommandSource silentSource = player.getCommandSource()
            .withLevel(4)
            .withOutput(CommandOutput.DUMMY);

        player.getServerWorld().getServer().getCommandManager()
            .executeWithPrefix(silentSource, "incapacitated setDowned false");

        resetDamageTracking(player);
        return true;
    }

    /**
     * Admin recovery path. Unlike safeRevive, this intentionally asks Incapacitated to rebuild its
     * revive state even when the downed flag is already false, then restores health. This is meant
     * for the observed "0 hearts after next damage" state.
     */
    public static void repairPlayer(ServerPlayerEntity player) {
        {
            ServerCommandSource silentSource = player.getCommandSource()
                .withLevel(4)
                .withOutput(CommandOutput.DUMMY);

            // Clear temporary crowd-control powers that can be left behind if a timer/source
            // chain is interrupted by death, disconnect, reload, or another mod's death hook.
            String[] transientControlPowers = {
                "andromeda_origins:arachne/helper/webbed",
                "andromeda_origins:gorgon/helper/constricted",
                "andromeda_origins:gorgon/helper/ophidianed",
                "andromeda_origins:siren/helper/infatuated",
                "andromeda_origins:common/petrified",
                "andromeda_origins:common/restrained",
                "andromeda_origins:common/silenced",
                "andromeda_origins:common/pacified"
            };

            for (String power : transientControlPowers) {
                player.getServerWorld().getServer().getCommandManager().executeWithPrefix(
                    silentSource,
                    "power revoke @s " + power + " minecraft:ccontrol"
                );
            }

            String[] controlResources = {
                "andromeda_origins:common/restrained_sources",
                "andromeda_origins:common/silenced_sources",
                "andromeda_origins:common/pacified_sources",
                "andromeda_origins:common/petrified_sources"
            };

            for (String resource : controlResources) {
                player.getServerWorld().getServer().getCommandManager().executeWithPrefix(
                    silentSource,
                    "resource set @s " + resource + " 0"
                );
            }

            if (isLoaded()) {
                player.getServerWorld().getServer().getCommandManager()
                    .executeWithPrefix(silentSource, "incapacitated setDowned false");
                resetDamageTracking(player);
            }
        }

        float maxHealth = player.getMaxHealth();
        if (Float.isFinite(maxHealth) && maxHealth > 0.0F) {
            player.setHealth(maxHealth);
        }

    }
}
