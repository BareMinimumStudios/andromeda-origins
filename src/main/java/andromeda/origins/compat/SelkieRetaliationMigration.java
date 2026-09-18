package andromeda.origins.compat;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Clears the pre-fix Selkie retaliation helper that could remain owned under minecraft:state. */
public final class SelkieRetaliationMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger("Andromeda Origins/Selkie Retaliation");
    private static final Identifier RETALIATED = Identifier.of("andromeda_origins", "selkie/helper/retaliated");
    private static final Identifier LEGACY_SOURCE = Identifier.of("minecraft", "state");

    private SelkieRetaliationMigration() {}

    public record Result(int legacyPowersRemoved, boolean successful) {}

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
                return new Result(0, false);
            }
            Object power = bridge.getPower(RETALIATED);
            if (power == null || !bridge.hasPower(power, LEGACY_SOURCE)) {
                return new Result(0, true);
            }
            if (bridge.removePower(power, LEGACY_SOURCE)) {
                bridge.sync();
                LOGGER.info("Removed stuck legacy Selkie retaliation debuff from {}.",
                    player.getGameProfile().getName());
                return new Result(1, true);
            }
            return new Result(0, true);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.error("Could not safely clear legacy Selkie retaliation state for {}.",
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
            Object component = componentClass.getMethod("getNullable", Entity.class).invoke(null, player);
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

        Object getPower(Identifier id) throws ReflectiveOperationException { return getPower.invoke(null, id); }
        boolean hasPower(Object power, Identifier source) throws ReflectiveOperationException {
            return Boolean.TRUE.equals(hasPower.invoke(component, power, source));
        }
        boolean removePower(Object power, Identifier source) throws ReflectiveOperationException {
            return Boolean.TRUE.equals(removePower.invoke(component, power, source));
        }
        void sync() throws ReflectiveOperationException { sync.invoke(component); }
    }
}
