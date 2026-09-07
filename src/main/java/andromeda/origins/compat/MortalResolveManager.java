package andromeda.origins.compat;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side lifetime tracking for Humanity's Mortal Resolve final stand.
 *
 * The timer deliberately lives outside Apoli's persistent power/resource state so a player who
 * dies during the final stand cannot respawn with a copied countdown and be killed a second time.
 * A command tag mirrors the active state so a normal disconnect cannot clear the final stand.
 */
public final class MortalResolveManager {

    public static final String ACTIVE_TAG = "andromeda_mortal_resolve";
    public static final String CHAMPION_ACTIVE_TAG = "andromeda_champion_mortal_resolve";
    public static final int DURATION_TICKS = 20 * 60;

    private static final Map<UUID, Integer> ACTIVE = new HashMap<>();
    private static final Map<UUID, Integer> CHAMPION_ACTIVE = new HashMap<>();

    private MortalResolveManager() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // If the server was restarted during a final stand, the vanilla command tag survives.
            // Re-arm the timer for online tagged players rather than silently dropping the state.
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player.getCommandTags().contains(ACTIVE_TAG) && !ACTIVE.containsKey(player.getUuid())) {
                    ACTIVE.put(player.getUuid(), DURATION_TICKS);
                }
                if (player.getCommandTags().contains(CHAMPION_ACTIVE_TAG)
                    && !CHAMPION_ACTIVE.containsKey(player.getUuid())) {
                    CHAMPION_ACTIVE.put(player.getUuid(), DURATION_TICKS);
                }
            }

            Iterator<Map.Entry<UUID, Integer>> iterator = ACTIVE.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Integer> entry = iterator.next();
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
                int remaining = entry.getValue();

                // Time continues to pass while disconnected. Once the player returns, an expired
                // final stand resolves immediately instead of allowing logout to avoid the death.
                if (player == null) {
                    entry.setValue(Math.max(0, remaining - 1));
                    continue;
                }

                // A real death before the timer expires already fulfills Mortal Resolve's finality.
                // The respawn callback below also clears the mirrored command tag.
                if (!player.isAlive()) {
                    player.removeCommandTag(ACTIVE_TAG);
                    iterator.remove();
                    continue;
                }

                if (!player.getCommandTags().contains(ACTIVE_TAG)) {
                    iterator.remove();
                    continue;
                }

                // Let the initial burst breathe for a moment, then leave a sparse upward trail
                // for the first 1.5 seconds. Five-tick spacing keeps this cosmetic effect cheap.
                int elapsed = DURATION_TICKS - remaining;
                if (elapsed >= 5 && elapsed <= 30 && elapsed % 5 == 0) {
                    MortalResolveEffects.playAfterglow(player);
                }

                // Refresh the short custom heartbeat at the Human's current position every 1.5s.
                // This avoids the old long positional sound becoming inaudible as the player moved.
                if (elapsed > 0 && elapsed % 30 == 0) {
                    MortalResolveEffects.playHeartbeatPulse(player);
                }

                if (remaining <= 1) {
                    iterator.remove();
                    player.removeCommandTag(ACTIVE_TAG);
                    IncapacitatedCompat.forceMortalResolveDeath(player);
                } else {
                    entry.setValue(remaining - 1);
                }
            }

            Iterator<Map.Entry<UUID, Integer>> championIterator = CHAMPION_ACTIVE.entrySet().iterator();
            while (championIterator.hasNext()) {
                Map.Entry<UUID, Integer> entry = championIterator.next();
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
                int remaining = entry.getValue();

                if (player == null) {
                    entry.setValue(Math.max(0, remaining - 1));
                    continue;
                }

                if (!player.isAlive() || !player.getCommandTags().contains(CHAMPION_ACTIVE_TAG)) {
                    player.removeCommandTag(CHAMPION_ACTIVE_TAG);
                    MortalResolveEffects.stopHeartbeat(player);
                    championIterator.remove();
                    continue;
                }

                int elapsed = DURATION_TICKS - remaining;
                if (elapsed >= 5 && elapsed <= 30 && elapsed % 5 == 0) {
                    MortalResolveEffects.playAfterglow(player);
                }
                if (elapsed > 0 && elapsed % 30 == 0) {
                    MortalResolveEffects.playHeartbeatPulse(player);
                }

                if (remaining <= 1) {
                    championIterator.remove();
                    player.removeCommandTag(CHAMPION_ACTIVE_TAG);
                    MortalResolveEffects.stopHeartbeat(player);
                } else {
                    entry.setValue(remaining - 1);
                }
            }
        });

        // Fabric fires this before ServerPlayerEntity's death processing. Preparing Incapacitated's
        // counter here means its own death hook sees Mortal Resolve deaths as final without needing
        // a cross-mod Mixin-order dependency.
        ServerPlayerEvents.ALLOW_DEATH.register((player, damageSource, damageAmount) -> {
            if (player.getCommandTags().contains(ACTIVE_TAG)) {
                MortalResolveEffects.stopHeartbeat(player);
                IncapacitatedCompat.prepareMortalResolveDeath(player);
            }
            if (player.getCommandTags().contains(CHAMPION_ACTIVE_TAG)) {
                stopChampion(player);
            }
            return true;
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            stop(newPlayer);
            stopChampion(newPlayer);
        });
    }

    public static boolean isActive(ServerPlayerEntity player) {
        return ACTIVE.containsKey(player.getUuid()) || player.getCommandTags().contains(ACTIVE_TAG);
    }

    public static void start(ServerPlayerEntity player) {
        ACTIVE.put(player.getUuid(), DURATION_TICKS);
        player.addCommandTag(ACTIVE_TAG);
        FiguraCompatManager.setResource(player, "andromeda_origins:humanity/figura_mortal_resolve", 1);
    }

    public static boolean isChampionActive(ServerPlayerEntity player) {
        return CHAMPION_ACTIVE.containsKey(player.getUuid())
            || player.getCommandTags().contains(CHAMPION_ACTIVE_TAG);
    }

    public static void startChampion(ServerPlayerEntity player) {
        CHAMPION_ACTIVE.put(player.getUuid(), DURATION_TICKS);
        player.addCommandTag(CHAMPION_ACTIVE_TAG);
        FiguraCompatManager.setResource(player, "andromeda_origins:humanity/figura_mortal_resolve", 1);
    }

    public static void stopChampion(ServerPlayerEntity player) {
        if (CHAMPION_ACTIVE.containsKey(player.getUuid())
            || player.getCommandTags().contains(CHAMPION_ACTIVE_TAG)) {
            MortalResolveEffects.stopHeartbeat(player);
        }
        CHAMPION_ACTIVE.remove(player.getUuid());
        player.removeCommandTag(CHAMPION_ACTIVE_TAG);
        FiguraCompatManager.setResource(player, "andromeda_origins:humanity/figura_mortal_resolve", 0);
    }

    public static void stop(ServerPlayerEntity player) {
        if (ACTIVE.containsKey(player.getUuid()) || player.getCommandTags().contains(ACTIVE_TAG)) {
            MortalResolveEffects.stopHeartbeat(player);
        }
        ACTIVE.remove(player.getUuid());
        player.removeCommandTag(ACTIVE_TAG);
        FiguraCompatManager.setResource(player, "andromeda_origins:humanity/figura_mortal_resolve", 0);
    }
}
