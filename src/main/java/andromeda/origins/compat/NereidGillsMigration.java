package andromeda.origins.compat;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Keeps standard Nereid's intrinsic gills attached under an Andromeda-owned power source.
 *
 * Older Nereid data granted origins:water_breathing from the generic minecraft:state source. That
 * source is also commonly used by temporary powers, so a revoke from an unrelated lifecycle could
 * remove the Nereid's own gills. Earlier v1.4.73 test builds also used a temporary
 * andromeda_origins:nereid_submersion gill source; current Submersion drains hostile air directly,
 * so that temporary source is now stale and is removed during this migration. Without the intrinsic
 * WaterBreathingPowerType, Origins' land-air drain does not run at all, making an affected Nereid
 * able to breathe normally on land until repaired.
 *
 * This migration is intentionally one-shot on join (and manually callable from /repair). It only
 * ensures the standard Nereid owns origins:water_breathing from andromeda_origins:nereid_gills;
 * Champion Nereid and every other Origin are kept free of that source. No tick callback is added.
 */
public final class NereidGillsMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger("Andromeda Origins/Nereid Gills");

    private static final Identifier NEREID_ORIGIN = Identifier.of("andromeda_origins", "nereid");
    private static final Identifier WATER_BREATHING = Identifier.of("origins", "water_breathing");
    private static final Identifier GILLS_SOURCE = Identifier.of("andromeda_origins", "nereid_gills");
    private static final Identifier LEGACY_SUBMERSION_GILLS_SOURCE = Identifier.of("andromeda_origins", "nereid_submersion");

    private NereidGillsMigration() {}

    public record Result(boolean nereid, int powersAdded, int powersRemoved, boolean successful) {
        public boolean changed() {
            return powersAdded > 0 || powersRemoved > 0;
        }
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            server.execute(() -> migratePlayer(player));
        });
    }

    public static Result migratePlayer(ServerPlayerEntity player) {
        boolean isNereid = false;
        try {
            isNereid = hasSelectedOrigin(player, NEREID_ORIGIN);

            ReflectionBridge bridge = ReflectionBridge.create(player);
            if (bridge == null) {
                LOGGER.warn("Could not access Apoli power data for {}; Nereid gills migration was skipped.",
                    player.getGameProfile().getName());
                return new Result(isNereid, 0, 0, false);
            }

            Object waterBreathing = bridge.getPower(WATER_BREATHING);
            if (waterBreathing == null) {
                LOGGER.warn("Could not resolve {} for {}; Nereid gills migration was skipped.",
                    WATER_BREATHING, player.getGameProfile().getName());
                return new Result(isNereid, 0, 0, false);
            }

            int added = 0;
            int removed = 0;

            // Current Submersion no longer grants Origins gills. Remove the temporary source
            // left by early v1.4.73 test builds regardless of the player's current Origin.
            if (bridge.hasPower(waterBreathing, LEGACY_SUBMERSION_GILLS_SOURCE)
                && bridge.removePower(waterBreathing, LEGACY_SUBMERSION_GILLS_SOURCE)) {
                removed++;
            }

            if (isNereid) {
                if (!bridge.hasPower(waterBreathing, GILLS_SOURCE) && bridge.addPower(waterBreathing, GILLS_SOURCE)) {
                    added = 1;
                }
            } else if (bridge.hasPower(waterBreathing, GILLS_SOURCE)
                && bridge.removePower(waterBreathing, GILLS_SOURCE)) {
                removed++;
            }

            if (added > 0 || removed > 0) {
                bridge.sync();
                if (added > 0) {
                    LOGGER.info("Restored intrinsic Nereid gills for {} without reselecting the Origin.",
                        player.getGameProfile().getName());
                } else {
                    LOGGER.info("Removed stale Nereid gills source from {}.",
                        player.getGameProfile().getName());
                }
            }

            return new Result(isNereid, added, removed, true);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.error("Could not safely reconcile Nereid gills for {}.",
                player.getGameProfile().getName(), unwrap(exception));
            return new Result(isNereid, 0, 0, false);
        }
    }

    private static boolean hasSelectedOrigin(ServerPlayerEntity player, Identifier target)
        throws ReflectiveOperationException {

        Class<?> modComponentsClass = Class.forName("io.github.apace100.origins.registry.ModComponents");
        Field originKeyField = modComponentsClass.getField("ORIGIN");
        Object originKey = originKeyField.get(null);

        Method getComponent = findCompatibleSingleArgMethod(originKeyField.getType(), "get", player);
        Object originComponent = getComponent.invoke(originKey, player);
        if (originComponent == null) {
            return false;
        }

        Method getOrigins = originComponent.getClass().getMethod("getOrigins");
        Object originsValue = getOrigins.invoke(originComponent);
        if (!(originsValue instanceof Map<?, ?> origins)) {
            return false;
        }

        for (Object origin : origins.values()) {
            if (origin == null) {
                continue;
            }
            Method getId = origin.getClass().getMethod("getId");
            Object idValue = getId.invoke(origin);
            if (target.equals(idValue)) {
                return true;
            }
        }

        return false;
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
        private final Method hasPower;
        private final Method addPower;
        private final Method removePower;
        private final Method sync;

        private ReflectionBridge(
            Object component,
            Method getPower,
            Method hasPower,
            Method addPower,
            Method removePower,
            Method sync
        ) {
            this.component = component;
            this.getPower = getPower;
            this.hasPower = hasPower;
            this.addPower = addPower;
            this.removePower = removePower;
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
                componentClass.getMethod("addPower", powerClass, Identifier.class),
                componentClass.getMethod("removePower", powerClass, Identifier.class),
                componentClass.getMethod("sync")
            );
        }

        Object getPower(Identifier id) throws ReflectiveOperationException {
            return getPower.invoke(null, id);
        }

        boolean hasPower(Object power, Identifier source) throws ReflectiveOperationException {
            return Boolean.TRUE.equals(hasPower.invoke(component, power, source));
        }

        boolean addPower(Object power, Identifier source) throws ReflectiveOperationException {
            return Boolean.TRUE.equals(addPower.invoke(component, power, source));
        }

        boolean removePower(Object power, Identifier source) throws ReflectiveOperationException {
            return Boolean.TRUE.equals(removePower.invoke(component, power, source));
        }

        void sync() throws ReflectiveOperationException {
            sync.invoke(component);
        }
    }
}
