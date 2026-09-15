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
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Repairs the v1.4.67+ iron-weakness multiple power for players whose Origin was selected before
 * new subpowers were added to that multiple power.
 *
 * Apoli persists the concrete powers a player owns. When a datapack update adds a new child to an
 * already-granted MultiplePower, reloading/restarting does not necessarily recurse back through the
 * parent and grant that new child. Re-selecting the Origin does, which is why affected legacy
 * players appeared to "fix themselves" after an Origin reselect.
 *
 * This migration is deliberately narrow:
 *  - it runs once per connection, not every tick;
 *  - it only considers the standard Faerie/Gorgon/Lichling Origins;
 *  - it only reconciles children of common/witheringironweakness;
 *  - it preserves existing powers/resources/cooldowns and only adds missing pieces;
 *  - it syncs Apoli only when something was actually added.
 *
 * The project intentionally avoids compile-linking Apoli/Origins internals. Reflection keeps the
 * existing compatibility boundary and fails closed if upstream internals are unavailable.
 */
public final class IronWeaknessMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger("Andromeda Origins/Iron Migration");

    private static final Identifier WITHERING_IRON_WEAKNESS =
        Identifier.of("andromeda_origins", "common/witheringironweakness");

    private static final Set<Identifier> AFFECTED_ORIGINS = Set.of(
        Identifier.of("andromeda_origins", "faerie"),
        Identifier.of("andromeda_origins", "gorgon"),
        Identifier.of("andromeda_origins", "lichling")
    );

    private IronWeaknessMigration() {}

    public record Result(boolean applicable, int powersAdded, boolean successful) {
        public boolean changed() {
            return powersAdded > 0;
        }
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            // Keep migration work on the server executor and outside packet handling. This is still
            // a one-shot join task; it does not register a tick callback or scan other entities.
            server.execute(() -> migratePlayer(player));
        });
    }

    /**
     * Reconciles only the current iron-weakness multiple power for an already-selected affected
     * Origin. Safe to call manually (for example from /andromedaorigins repair) as well as on join.
     */
    public static Result migratePlayer(ServerPlayerEntity player) {
        try {
            Identifier originId = getCurrentAffectedOrigin(player);
            if (originId == null) {
                return new Result(false, 0, true);
            }

            ReflectionBridge bridge = ReflectionBridge.create(player);
            if (bridge == null) {
                LOGGER.warn("Could not access Apoli power data for {}; iron-weakness migration was skipped.",
                    player.getGameProfile().getName());
                return new Result(true, 0, false);
            }

            Object parentPower = bridge.getPower(WITHERING_IRON_WEAKNESS);
            if (parentPower == null) {
                LOGGER.warn("Could not resolve {} for {}; iron-weakness migration was skipped.",
                    WITHERING_IRON_WEAKNESS, player.getGameProfile().getName());
                return new Result(true, 0, false);
            }

            int added = 0;

            // If even the parent is missing for the currently selected Origin, granting the parent
            // lets Apoli perform its normal recursive MultiplePower initialization.
            if (!bridge.hasPower(parentPower, originId)) {
                if (bridge.addPower(parentPower, originId)) {
                    added++;
                }
            } else {
                // Legacy players generally still own the parent. Reconcile the parent's current
                // children directly because re-adding an already-owned parent returns early in
                // Apoli and therefore does not recurse into newly-added children.
                for (Object childPower : bridge.getSubPowers(parentPower)) {
                    if (!bridge.hasPower(childPower, originId) && bridge.addPower(childPower, originId)) {
                        added++;
                    }
                }
            }

            if (added > 0) {
                bridge.sync();
                LOGGER.info("Migrated {} missing iron-weakness power piece(s) for {} ({}) without reselecting the Origin.",
                    added, player.getGameProfile().getName(), originId);
            }

            return new Result(true, added, true);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.error("Could not safely migrate iron weakness for {}; no Origin reselect was attempted.",
                player.getGameProfile().getName(), unwrap(exception));
            return new Result(true, 0, false);
        }
    }

    private static Identifier getCurrentAffectedOrigin(ServerPlayerEntity player)
        throws ReflectiveOperationException {

        Class<?> modComponentsClass = Class.forName("io.github.apace100.origins.registry.ModComponents");
        Field originKeyField = modComponentsClass.getField("ORIGIN");
        Object originKey = originKeyField.get(null);

        Method getComponent = findCompatibleSingleArgMethod(originKeyField.getType(), "get", player);
        Object originComponent = getComponent.invoke(originKey, player);
        if (originComponent == null) {
            return null;
        }

        Method getOrigins = originComponent.getClass().getMethod("getOrigins");
        Object originsValue = getOrigins.invoke(originComponent);
        if (!(originsValue instanceof Map<?, ?> origins)) {
            return null;
        }

        for (Object origin : origins.values()) {
            if (origin == null) {
                continue;
            }

            Method getId = origin.getClass().getMethod("getId");
            Object idValue = getId.invoke(origin);
            if (idValue instanceof Identifier id && AFFECTED_ORIGINS.contains(id)) {
                return id;
            }
        }

        return null;
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
        private final Method sync;

        private ReflectionBridge(
            Object component,
            Method getPower,
            Method hasPower,
            Method addPower,
            Method sync
        ) {
            this.component = component;
            this.getPower = getPower;
            this.hasPower = hasPower;
            this.addPower = addPower;
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
