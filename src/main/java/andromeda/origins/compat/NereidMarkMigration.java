package andromeda.origins.compat;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Clears the pre-v1.4.73-revision Nereid ally mark source from players.
 *
 * Older Nereid marking granted the seafriend helper from the generic minecraft:state source with
 * no expiry. Current marks use the dedicated andromeda_origins:nereid_mark source and carry their
 * own 15-second timer. Removing only the old source on join prevents an indefinitely marked player
 * from bypassing the new timer while leaving a legitimate current timed mark untouched.
 */
public final class NereidMarkMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger("Andromeda Origins/Nereid Mark");

    private static final Identifier SEA_FRIEND = Identifier.of("andromeda_origins", "nereid/helper/seafriend");
    private static final Identifier LEGACY_SOURCE = Identifier.of("minecraft", "state");

    private NereidMarkMigration() {}

    public record Result(int legacyMarksRemoved, boolean successful) {
        public boolean changed() {
            return legacyMarksRemoved > 0;
        }
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            server.execute(() -> migratePlayer(player));
        });
    }

    public static Result migratePlayer(ServerPlayerEntity player) {
        try {
            ReflectionBridge bridge = ReflectionBridge.create(player);
            if (bridge == null) {
                LOGGER.warn("Could not access Apoli power data for {}; legacy Nereid mark cleanup was skipped.",
                    player.getGameProfile().getName());
                return new Result(0, false);
            }

            Object mark = bridge.getPower(SEA_FRIEND);
            if (mark == null) {
                // A missing helper is harmless and can occur while datapacks are still being rebuilt.
                return new Result(0, true);
            }

            int removed = 0;
            if (bridge.hasPower(mark, LEGACY_SOURCE) && bridge.removePower(mark, LEGACY_SOURCE)) {
                removed = 1;
                bridge.sync();
                LOGGER.info("Removed indefinite legacy Nereid ally mark from {}.",
                    player.getGameProfile().getName());
            }

            return new Result(removed, true);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.error("Could not safely clear the legacy Nereid ally mark for {}.",
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
        private final Method getPower;
        private final Method hasPower;
        private final Method removePower;
        private final Method sync;

        private ReflectionBridge(Object component, Method getPower, Method hasPower, Method removePower, Method sync) {
            this.component = component;
            this.getPower = getPower;
            this.hasPower = hasPower;
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

        boolean removePower(Object power, Identifier source) throws ReflectiveOperationException {
            return Boolean.TRUE.equals(removePower.invoke(component, power, source));
        }

        void sync() throws ReflectiveOperationException {
            sync.invoke(component);
        }
    }
}
