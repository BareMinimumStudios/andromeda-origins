package andromeda.origins.compat;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Keeps standard Nereid's land-suffocation state migration-safe and self-consistent.
 *
 * <p>Earlier builds granted Origins' gill power indirectly from temporary helper sources. That made
 * an intrinsic racial trait vulnerable to callback ordering, power reloads, and generic state cleanup.
 * v1.4.75 instead gives standard Nereid a hidden, direct WaterBreathingPowerType power owned by the
 * selected Origin itself ({@code andromeda_origins:nereid/gills} from source
 * {@code andromeda_origins:nereid}). This migration repairs existing players into that ownership
 * model and removes the old temporary gill sources.</p>
 *
 * <p>The migration also reconciles children of Nereid's passive MultiplePower. Apoli persists the
 * concrete child powers a player owns, so a legacy player can otherwise keep the parent while
 * missing a current child such as the once-per-second hydration/dry-state controller. Missing
 * children are added without reselecting the Origin.</p>
 *
 * <p>Finally, an orphaned shared Wet power is removed only when a standard Nereid has no Nereid Wet
 * helper owning that hydration. This repairs a legacy state where hydration could remain forever
 * and continuously mask land air loss.</p>
 *
 * <p>Runs after Origins has synchronized origin layers on join/data-pack sync and is also callable
 * from /andromedaorigins repair. There is no recurring tick scan.</p>
 */
public final class NereidGillsMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger("Andromeda Origins/Nereid Breathing");

    private static final Identifier MIGRATION_PHASE = Identifier.of("andromeda_origins", "nereid_breathing_migration");
    private static final Identifier ORIGIN_LAYER_SYNC_PHASE = Identifier.of("origins", "origin_layers");

    private static final Identifier NEREID_ORIGIN = Identifier.of("andromeda_origins", "nereid");
    private static final Identifier CHAMPION_NEREID_ORIGIN = Identifier.of("andromeda_origins", "champion_nereid");
    private static final Identifier NEREID_PASSIVES = Identifier.of("andromeda_origins", "nereid/passives");
    private static final Identifier NEREID_GILLS = Identifier.of("andromeda_origins", "nereid/gills");
    private static final Identifier NEREID_WET = Identifier.of("andromeda_origins", "nereid/helper/nereidwet");
    private static final Identifier NEREID_WET_REMOVE = Identifier.of("andromeda_origins", "nereid/helper/nereidwet_remove");
    private static final Identifier NEREID_WET_TIMER = Identifier.of("andromeda_origins", "nereid/helper/nereidwet_timer");
    private static final Identifier COMMON_WET = Identifier.of("andromeda_origins", "common/wet");

    private static final Identifier LEGACY_WATER_BREATHING = Identifier.of("origins", "water_breathing");
    private static final Identifier LEGACY_GILLS_SOURCE = Identifier.of("andromeda_origins", "nereid_gills");
    private static final Identifier LEGACY_SUBMERSION_GILLS_SOURCE = Identifier.of("andromeda_origins", "nereid_submersion");
    private static final Identifier LEGACY_STATE_SOURCE = Identifier.of("minecraft", "state");
    private static final Identifier WET_SOURCE = Identifier.of("minecraft", "debuff");

    private NereidGillsMigration() {}

    public record Result(boolean nereid, int powersAdded, int powersRemoved, boolean successful) {
        public boolean changed() {
            return powersAdded > 0 || powersRemoved > 0;
        }
    }

    public static void register() {
        // Origins updates its Origin component in the origins:origin_layers phase. Running after
        // that phase avoids making a destructive decision from a temporarily incomplete selection.
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.addPhaseOrdering(ORIGIN_LAYER_SYNC_PHASE, MIGRATION_PHASE);
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register(MIGRATION_PHASE, (player, joined) -> migratePlayer(player, joined));
    }

    public static Result migratePlayer(ServerPlayerEntity player) {
        return migratePlayer(player, false);
    }

    /**
     * Reconciles Nereid breathing state. When {@code joined} is true, any stored Nereid Wet
     * hydration from the previous connection is discarded before normal reconciliation. Wet is a
     * short-lived support/combat buff and must never be banked across logout/reconnect; natural
     * water/rain hydration is live-state driven and can immediately reapply after joining.
     */
    private static Result migratePlayer(ServerPlayerEntity player, boolean joined) {
        boolean isNereid = false;
        try {
            ReflectionBridge bridge = ReflectionBridge.create(player);
            if (bridge == null) {
                LOGGER.warn("Could not access Apoli power data for {}; Nereid breathing reconciliation was skipped.",
                    player.getGameProfile().getName());
                return new Result(false, 0, 0, false);
            }

            int added = 0;
            int removed = 0;

            // Stored Nereid Wet is deliberately session-scoped. Apoli resource values and power
            // ownership persist through logout, so a player could reconnect with a partially filled
            // hydration bar and keep common/wet alive indefinitely if its parent/timer ownership
            // desynchronized. Clear both the Nereid hydration owner and its shared Wet ownership on
            // a real join before consulting Origin selection. This is safe even if Origins has not
            // finished resolving the selected layer yet: no freshly joined player can have earned a
            // new Nereid Wet buff in the current session. Datapack reloads and /repair pass joined=false
            // and therefore never erase legitimate live hydration.
            if (joined) {
                Object joinedNereidWet = bridge.getPower(NEREID_WET);
                Object joinedCommonWet = bridge.getPower(COMMON_WET);
                boolean removedStoredNereidWet = false;
                if (joinedNereidWet != null) {
                    int removedWetOwner = bridge.removeIfOwned(joinedNereidWet, WET_SOURCE);
                    removed += removedWetOwner;
                    removedStoredNereidWet = removedWetOwner > 0;
                }
                // common/wet is shared with Selkie, so only remove its shared debuff ownership
                // when this join actually removed a persisted Nereid Wet owner. Never clear a
                // Selkie's legitimate Wet state merely because this migration runs for all players.
                if (removedStoredNereidWet && joinedCommonWet != null) {
                    removed += bridge.removeIfOwned(joinedCommonWet, WET_SOURCE);
                }
            }

            Set<Identifier> selectedOrigins = getSelectedOrigins(player);

            // Fail closed if the Origin component has not produced any selection data yet. In
            // particular, never remove gills based on a transient negative lookup during login.
            // Session hydration cleanup above is independent of Origin selection and is intentionally
            // still allowed because it only discards a previous connection's temporary Wet state.
            if (selectedOrigins.isEmpty()) {
                if (removed > 0) {
                    bridge.sync();
                    LOGGER.info("Cleared persisted Nereid hydration state for {} on join.",
                        player.getGameProfile().getName());
                }
                return new Result(false, 0, removed, true);
            }

            isNereid = selectedOrigins.contains(NEREID_ORIGIN);
            boolean isChampionNereid = selectedOrigins.contains(CHAMPION_NEREID_ORIGIN);

            Object legacyWaterBreathing = bridge.getPower(LEGACY_WATER_BREATHING);
            Object directGills = bridge.getPower(NEREID_GILLS);
            Object passives = bridge.getPower(NEREID_PASSIVES);

            if (legacyWaterBreathing == null || directGills == null || passives == null) {
                LOGGER.warn("Could not resolve one or more Nereid breathing powers for {}; reconciliation was skipped.",
                    player.getGameProfile().getName());
                if (removed > 0) {
                    bridge.sync();
                }
                return new Result(isNereid, 0, removed, false);
            }

            // Old v1.4.72-v1.4.74 ownership. These sources belong specifically to Andromeda's
            // previous Nereid implementation and are always safe to remove once selection is known.
            removed += bridge.removeIfOwned(legacyWaterBreathing, LEGACY_GILLS_SOURCE);
            removed += bridge.removeIfOwned(legacyWaterBreathing, LEGACY_SUBMERSION_GILLS_SOURCE);

            if (isNereid) {
                // Standard Nereid owns gills directly from the selected Origin. This source is
                // removed by Origins itself when the player genuinely changes Origin.
                if (!bridge.hasPower(directGills, NEREID_ORIGIN) && bridge.addPower(directGills, NEREID_ORIGIN)) {
                    added++;
                }

                // Normalize the old helper-owned vanilla gill power. The direct hidden Nereid
                // power above uses the same WaterBreathingPowerType without temporary ownership.
                removed += bridge.removeIfOwned(legacyWaterBreathing, LEGACY_STATE_SOURCE);

                // Reconcile the complete current child set of Nereid passives. This protects old
                // players from stale MultiplePower membership without resetting existing children.
                if (!bridge.hasPower(passives, NEREID_ORIGIN)) {
                    if (bridge.addPower(passives, NEREID_ORIGIN)) {
                        added++;
                    }
                } else {
                    for (Object childPower : bridge.getSubPowers(passives)) {
                        if (!bridge.hasPower(childPower, NEREID_ORIGIN) && bridge.addPower(childPower, NEREID_ORIGIN)) {
                            added++;
                        }
                    }
                }

                // A stale common Wet source can keep Nereid hydrated forever and continually mask
                // gill air loss. If no Nereid Wet helper exists, that debuff ownership is orphaned.
                Object commonWet = bridge.getPower(COMMON_WET);
                Object nereidWet = bridge.getPower(NEREID_WET);
                Object nereidWetRemove = bridge.getPower(NEREID_WET_REMOVE);
                Object nereidWetTimer = bridge.getPower(NEREID_WET_TIMER);

                if (commonWet != null && nereidWet != null
                    && bridge.hasPower(commonWet, WET_SOURCE)
                    && !bridge.hasPower(nereidWet)) {
                    removed += bridge.removeIfOwned(commonWet, WET_SOURCE);
                }

                // A legacy Nereid Wet parent can itself have stale MultiplePower membership. Ensure
                // its duration resource and countdown timer exist so legitimate hydration always
                // expires instead of becoming permanent.
                if (nereidWet != null && bridge.hasPower(nereidWet)
                    && nereidWetRemove != null && nereidWetTimer != null) {
                    for (Identifier source : bridge.getSources(nereidWet)) {
                        if (!bridge.hasPower(nereidWetRemove, source) && bridge.addPower(nereidWetRemove, source)) {
                            added++;
                        }
                        if (!bridge.hasPower(nereidWetTimer, source) && bridge.addPower(nereidWetTimer, source)) {
                            added++;
                        }
                    }
                }

                player.addCommandTag("nereid");
            } else {
                // Direct gills should never exist on a non-standard Nereid. This check runs only
                // after a non-empty, post-Origin-sync selection was observed.
                removed += bridge.removeIfOwned(directGills, NEREID_ORIGIN);

                // Champion Nereid intentionally keeps aquatic utility without the standard land
                // suffocation weakness. Remove any old helper-owned gills left from pre-v1.4.75.
                if (isChampionNereid) {
                    removed += bridge.removeIfOwned(legacyWaterBreathing, LEGACY_STATE_SOURCE);
                } else {
                    player.removeCommandTag("nereid");
                }
            }

            if (added > 0 || removed > 0) {
                bridge.sync();
                LOGGER.info("Reconciled Nereid breathing state for {} (added {}, removed {}).",
                    player.getGameProfile().getName(), added, removed);
            }

            return new Result(isNereid, added, removed, true);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.error("Could not safely reconcile Nereid breathing state for {}.",
                player.getGameProfile().getName(), unwrap(exception));
            return new Result(isNereid, 0, 0, false);
        }
    }

    private static Set<Identifier> getSelectedOrigins(ServerPlayerEntity player)
        throws ReflectiveOperationException {

        Class<?> modComponentsClass = Class.forName("io.github.apace100.origins.registry.ModComponents");
        Field originKeyField = modComponentsClass.getField("ORIGIN");
        Object originKey = originKeyField.get(null);

        Method getComponent = findCompatibleSingleArgMethod(originKeyField.getType(), "get", player);
        Object originComponent = getComponent.invoke(originKey, player);
        if (originComponent == null) {
            return Set.of();
        }

        Method getOrigins = originComponent.getClass().getMethod("getOrigins");
        Object originsValue = getOrigins.invoke(originComponent);
        if (!(originsValue instanceof Map<?, ?> origins)) {
            return Set.of();
        }

        Set<Identifier> selected = new HashSet<>();
        for (Object origin : origins.values()) {
            if (origin == null) {
                continue;
            }
            Method getId = origin.getClass().getMethod("getId");
            Object idValue = getId.invoke(origin);
            if (idValue instanceof Identifier id) {
                selected.add(id);
            }
        }
        return selected;
    }

    private static Method findCompatibleSingleArgMethod(Class<?> ownerClass, String name, Object argument)
        throws NoSuchMethodException {

        for (Method method : ownerClass.getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != 1) {
                continue;
            }
            if (method.getParameterTypes()[0].isInstance(argument)) {
                return method;
            }
        }

        throw new NoSuchMethodException(ownerClass.getName() + "#" + name + "(compatible single argument)");
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof InvocationTargetException invocation && invocation.getCause() != null) {
            return invocation.getCause();
        }
        return throwable;
    }

    private static final class ReflectionBridge {
        private final Object component;
        private final Method getPower;
        private final Method hasPowerWithSource;
        private final Method hasPower;
        private final Method addPower;
        private final Method removePower;
        private final Method getSources;
        private final Method sync;

        private ReflectionBridge(
            Object component,
            Method getPower,
            Method hasPowerWithSource,
            Method hasPower,
            Method addPower,
            Method removePower,
            Method getSources,
            Method sync
        ) {
            this.component = component;
            this.getPower = getPower;
            this.hasPowerWithSource = hasPowerWithSource;
            this.hasPower = hasPower;
            this.addPower = addPower;
            this.removePower = removePower;
            this.getSources = getSources;
            this.sync = sync;
        }

        static ReflectionBridge create(ServerPlayerEntity player) throws ReflectiveOperationException {
            Class<?> componentClass = Class.forName("io.github.apace100.apoli.component.PowerHolderComponent");
            Class<?> powerClass = Class.forName("io.github.apace100.apoli.power.Power");
            Class<?> powerManagerClass = Class.forName("io.github.apace100.apoli.power.PowerManager");

            Method getNullable = componentClass.getMethod("getNullable", Entity.class);
            Object component = getNullable.invoke(null, player);
            if (component == null) {
                return null;
            }

            return new ReflectionBridge(
                component,
                powerManagerClass.getMethod("getNullable", Identifier.class),
                componentClass.getMethod("hasPower", powerClass, Identifier.class),
                componentClass.getMethod("hasPower", powerClass),
                componentClass.getMethod("addPower", powerClass, Identifier.class),
                componentClass.getMethod("removePower", powerClass, Identifier.class),
                componentClass.getMethod("getSources", powerClass),
                componentClass.getMethod("sync")
            );
        }

        Object getPower(Identifier id) throws ReflectiveOperationException {
            return getPower.invoke(null, id);
        }

        boolean hasPower(Object power, Identifier source) throws ReflectiveOperationException {
            return Boolean.TRUE.equals(hasPowerWithSource.invoke(component, power, source));
        }

        boolean hasPower(Object power) throws ReflectiveOperationException {
            return Boolean.TRUE.equals(hasPower.invoke(component, power));
        }

        boolean addPower(Object power, Identifier source) throws ReflectiveOperationException {
            return Boolean.TRUE.equals(addPower.invoke(component, power, source));
        }

        int removeIfOwned(Object power, Identifier source) throws ReflectiveOperationException {
            return hasPower(power, source) && Boolean.TRUE.equals(removePower.invoke(component, power, source)) ? 1 : 0;
        }

        Set<Identifier> getSources(Object power) throws ReflectiveOperationException {
            Object value = getSources.invoke(component, power);
            if (!(value instanceof Iterable<?> iterable)) {
                return Set.of();
            }
            Set<Identifier> result = new HashSet<>();
            for (Object source : iterable) {
                if (source instanceof Identifier id) {
                    result.add(id);
                }
            }
            return result;
        }

        Collection<?> getSubPowers(Object parentPower) throws ReflectiveOperationException {
            try {
                Method getSubPowers = parentPower.getClass().getMethod("getSubPowers");
                Object value = getSubPowers.invoke(parentPower);
                return value instanceof Collection<?> collection ? collection : Set.of();
            } catch (NoSuchMethodException exception) {
                return Set.of();
            }
        }

        void sync() throws ReflectiveOperationException {
            sync.invoke(component);
        }
    }
}
