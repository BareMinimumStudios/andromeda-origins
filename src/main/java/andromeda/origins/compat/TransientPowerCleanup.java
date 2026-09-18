package andromeda.origins.compat;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

/**
 * Replaces the old common/debug behavior that revoked every power from several generic Apoli
 * sources. That legacy cleanup could erase temporary powers owned by unrelated Origins addons.
 *
 * This cleanup keeps the same intent, but only removes Andromeda-owned powers from the generic
 * transient source IDs Andromeda historically used. common/debug itself is excluded so its JSON
 * callback can revoke itself after this command returns without re-entrant removal.
 */
public final class TransientPowerCleanup {

    private static final Logger LOGGER = LoggerFactory.getLogger("Andromeda Origins/Transient Cleanup");
    private static final String ANDROMEDA_NAMESPACE = "andromeda_origins";
    private static final Identifier DEBUG_POWER = Identifier.of(ANDROMEDA_NAMESPACE, "common/debug");
    private static final Set<Identifier> TRANSIENT_SOURCES = Set.of(
        Identifier.of("minecraft", "buff"),
        Identifier.of("minecraft", "debuff"),
        Identifier.of("minecraft", "ccontrol"),
        Identifier.of("minecraft", "state")
    );

    private TransientPowerCleanup() {}

    public record Result(int powersRemoved, boolean successful) {}

    public static Result cleanup(ServerPlayerEntity player) {
        try {
            ReflectionBridge bridge = ReflectionBridge.create(player);
            if (bridge == null) {
                return new Result(0, false);
            }

            int removed = bridge.removeAndromedaTransientPowers();
            if (removed > 0) {
                bridge.sync();
                LOGGER.debug("Removed {} Andromeda transient power ownership(s) from {}.",
                    removed, player.getGameProfile().getName());
            }
            return new Result(removed, true);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.error("Could not safely clear Andromeda transient powers for {}.",
                player.getGameProfile().getName(), unwrap(exception));
            return new Result(0, false);
        }
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof InvocationTargetException invocation && invocation.getCause() != null) {
            return invocation.getCause();
        }
        return throwable;
    }

    private static final class ReflectionBridge {
        private final Object component;
        private final Method getPowers;
        private final Method getSources;
        private final Method powerGetId;
        private final Method removePower;
        private final Method sync;

        private ReflectionBridge(
            Object component,
            Method getPowers,
            Method getSources,
            Method powerGetId,
            Method removePower,
            Method sync
        ) {
            this.component = component;
            this.getPowers = getPowers;
            this.getSources = getSources;
            this.powerGetId = powerGetId;
            this.removePower = removePower;
            this.sync = sync;
        }

        static ReflectionBridge create(ServerPlayerEntity player) throws ReflectiveOperationException {
            Class<?> componentClass = Class.forName("io.github.apace100.apoli.component.PowerHolderComponent");
            Class<?> powerClass = Class.forName("io.github.apace100.apoli.power.Power");
            Method getNullable = componentClass.getMethod("getNullable", Entity.class);
            Object component = getNullable.invoke(null, player);
            if (component == null) {
                return null;
            }

            return new ReflectionBridge(
                component,
                componentClass.getMethod("getPowers", boolean.class),
                componentClass.getMethod("getSources", powerClass),
                powerClass.getMethod("getId"),
                componentClass.getMethod("removePower", powerClass, Identifier.class),
                componentClass.getMethod("sync")
            );
        }

        int removeAndromedaTransientPowers() throws ReflectiveOperationException {
            Object value = getPowers.invoke(component, true);
            if (!(value instanceof Iterable<?> powers)) {
                return 0;
            }

            ArrayList<Object> snapshot = new ArrayList<>();
            powers.forEach(snapshot::add);
            int removed = 0;

            for (Object power : snapshot) {
                Object idValue = powerGetId.invoke(power);
                if (!(idValue instanceof Identifier powerId)
                    || !ANDROMEDA_NAMESPACE.equals(powerId.getNamespace())
                    || DEBUG_POWER.equals(powerId)) {
                    continue;
                }

                Object sourcesValue = getSources.invoke(component, power);
                if (!(sourcesValue instanceof Iterable<?> sources)) {
                    continue;
                }

                ArrayList<Identifier> sourceSnapshot = new ArrayList<>();
                for (Object source : sources) {
                    if (source instanceof Identifier id && TRANSIENT_SOURCES.contains(id)) {
                        sourceSnapshot.add(id);
                    }
                }

                for (Identifier source : sourceSnapshot) {
                    if (Boolean.TRUE.equals(removePower.invoke(component, power, source))) {
                        removed++;
                    }
                }
            }

            return removed;
        }

        void sync() throws ReflectiveOperationException {
            sync.invoke(component);
        }
    }
}
