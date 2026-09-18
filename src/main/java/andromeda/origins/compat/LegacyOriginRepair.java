package andromeda.origins.compat;

import andromeda.origins.mixin.AttributeContainerAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Removes stale power/attribute state left by previously selected Origin mods before Andromeda's
 * normal attribute rebuild runs.
 *
 * Origins uses the selected Origin ID as the Apoli power source. Removing a stale source therefore
 * also removes vanilla powers that an old Origin granted (for example origins:carnivore) without
 * having to hard-code every power in that old mod. Registered stale Origin sources are cleaned
 * generically; the removed Medieval Origins namespace is recognized explicitly so old saves can be
 * repaired even when Medieval Origins is no longer installed/registered.
 */
public final class LegacyOriginRepair {

    private static final Logger LOGGER = LoggerFactory.getLogger("Andromeda Origins/Legacy Origin Repair");
    private static final Set<String> KNOWN_REMOVED_ORIGIN_NAMESPACES = Set.of("medievalorigins");
    private static final Set<Identifier> LEGACY_DIET_POWERS = Set.of(
        Identifier.of("origins", "carnivore"),
        Identifier.of("origins", "vegetarian")
    );
    private static final String MEDIEVAL_SIREN_TAG = "siren_seduce";

    private LegacyOriginRepair() {}

    public record Result(
        int staleSourcesRemoved,
        int powersRemoved,
        int orphanedModifiersRemoved,
        boolean pehkuiEyeHeightReset,
        boolean successful
    ) {
        public boolean changed() {
            return staleSourcesRemoved > 0 || powersRemoved > 0 || orphanedModifiersRemoved > 0 || pehkuiEyeHeightReset;
        }
    }

    public static Result repair(ServerPlayerEntity player) {
        try {
            Set<Identifier> selectedOrigins = getSelectedOrigins(player);
            if (selectedOrigins.isEmpty()) {
                LOGGER.warn("Refusing legacy Origin cleanup for {} because the current Origin selection could not be resolved safely.",
                    player.getGameProfile().getName());
                return new Result(0, 0, 0, false, false);
            }

            ReflectionBridge bridge = ReflectionBridge.create(player);
            if (bridge == null) {
                LOGGER.warn("Could not access Apoli power data for {}; legacy Origin cleanup was skipped.",
                    player.getGameProfile().getName());
                return new Result(0, 0, 0, false, false);
            }

            int staleSourcesRemoved = 0;
            int powersRemoved = 0;

            // Capture Pixie evidence before any cleanup removes the source/powers/modifiers.
            // Pehkui eye-height is global player state, so resetting it without evidence would be
            // too aggressive and could overwrite a legitimate scale owned by another current mod.
            boolean medievalPixieResidue = bridge.hasSource(Identifier.of("medievalorigins", "pixie"))
                || bridge.hasPowerPathPrefix("medievalorigins", "pixie/")
                || hasMedievalPixieModifier(player);

            // Do not strip Medieval state if the player is intentionally still using a Medieval
            // Origin on another layer. This is mainly a safety guard for mixed test packs.
            boolean currentlyMedieval = selectedOrigins.stream()
                .anyMatch(id -> "medievalorigins".equals(id.getNamespace()));

            if (!currentlyMedieval) {
                for (Identifier source : bridge.getAllSources()) {
                    if (selectedOrigins.contains(source)) {
                        continue;
                    }

                    boolean knownRemovedNamespace = KNOWN_REMOVED_ORIGIN_NAMESPACES.contains(source.getNamespace());
                    boolean registeredOrigin = bridge.isRegisteredOrigin(source);
                    if (!knownRemovedNamespace && !registeredOrigin) {
                        continue;
                    }

                    int removed = bridge.removeAllFromSource(source);
                    if (removed > 0) {
                        staleSourcesRemoved++;
                        powersRemoved += removed;
                        LOGGER.info("Removed stale Origin power source {} from {} ({} top-level powers).",
                            source, player.getGameProfile().getName(), removed);
                    }
                }

                // Medieval Origins also granted several temporary/helper powers through sources such
                // as apoli:command. Remove Medieval-namespaced powers from any remaining source, then
                // clean stale vanilla diet powers from non-selected sources. This catches residue the
                // source-only pass cannot see while preserving diet ownership from the current Origin.
                powersRemoved += bridge.removePowersInNamespace("medievalorigins");
                powersRemoved += bridge.removePowerFromLegacySources(LEGACY_DIET_POWERS, selectedOrigins);
            }

            if (powersRemoved > 0) {
                bridge.sync();
            }

            int orphanedModifiersRemoved = currentlyMedieval ? 0 : removeMedievalAttributeModifiers(player);
            boolean pehkuiReset = !currentlyMedieval && medievalPixieResidue
                && resetMedievalPixieEyeHeight(player);

            // Medieval Siren used this command tag transiently. It is safe to clear when repairing a
            // player who is no longer on a Medieval Origin.
            if (!currentlyMedieval) {
                player.removeCommandTag(MEDIEVAL_SIREN_TAG);
            }

            return new Result(staleSourcesRemoved, powersRemoved, orphanedModifiersRemoved, pehkuiReset, true);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.error("Could not safely clean legacy Origin state for {}.",
                player.getGameProfile().getName(), unwrap(exception));
            return new Result(0, 0, 0, false, false);
        }
    }

    private static boolean hasMedievalPixieModifier(ServerPlayerEntity player) {
        if (!(player.getAttributes() instanceof AttributeContainerAccessor accessor)) {
            return false;
        }

        for (EntityAttributeInstance instance : accessor.andromeda$getCustomAttributes().values()) {
            for (EntityAttributeModifier modifier : instance.getModifiers()) {
                Identifier id = modifier.id();
                if ("medievalorigins".equals(id.getNamespace()) && id.getPath().startsWith("pixie/")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int removeMedievalAttributeModifiers(ServerPlayerEntity player) {
        if (!(player.getAttributes() instanceof AttributeContainerAccessor accessor)) {
            LOGGER.warn("AttributeContainer accessor was unavailable for {}; orphaned Medieval modifiers were not scanned.",
                player.getGameProfile().getName());
            return 0;
        }

        int removed = 0;
        for (EntityAttributeInstance instance : accessor.andromeda$getCustomAttributes().values()) {
            for (EntityAttributeModifier modifier : new ArrayList<>(instance.getModifiers())) {
                if ("medievalorigins".equals(modifier.id().getNamespace())) {
                    instance.removeModifier(modifier.id());
                    removed++;
                }
            }
        }

        if (removed > 0) {
            LOGGER.info("Removed {} orphaned Medieval Origins attribute modifier(s) from {}.",
                removed, player.getGameProfile().getName());
        }
        return removed;
    }

    private static boolean resetMedievalPixieEyeHeight(ServerPlayerEntity player) {
        if (!FabricLoader.getInstance().isModLoaded("pehkui")) {
            return false;
        }

        try {
            ServerCommandSource source = player.getCommandSource().withLevel(4).withOutput(CommandOutput.DUMMY);
            player.getServerWorld().getServer().getCommandManager()
                .executeWithPrefix(source, "scale reset pehkui:eye_height @s");
            return true;
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not reset Medieval Pixie's Pehkui eye-height scale for {}.",
                player.getGameProfile().getName(), exception);
            return false;
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
        private final Method getPowers;
        private final Method getSources;
        private final Method powerGetId;
        private final Method removePower;
        private final Method removeAllPowersFromSource;
        private final Method sync;
        private final Method originManagerContains;

        private ReflectionBridge(
            Object component,
            Method getPowers,
            Method getSources,
            Method powerGetId,
            Method removePower,
            Method removeAllPowersFromSource,
            Method sync,
            Method originManagerContains
        ) {
            this.component = component;
            this.getPowers = getPowers;
            this.getSources = getSources;
            this.powerGetId = powerGetId;
            this.removePower = removePower;
            this.removeAllPowersFromSource = removeAllPowersFromSource;
            this.sync = sync;
            this.originManagerContains = originManagerContains;
        }

        static ReflectionBridge create(ServerPlayerEntity player) throws ReflectiveOperationException {
            Class<?> componentClass = Class.forName("io.github.apace100.apoli.component.PowerHolderComponent");
            Class<?> powerClass = Class.forName("io.github.apace100.apoli.power.Power");
            Class<?> originManagerClass = Class.forName("io.github.apace100.origins.origin.OriginManager");

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
                componentClass.getMethod("removeAllPowersFromSource", Identifier.class),
                componentClass.getMethod("sync"),
                originManagerClass.getMethod("contains", Identifier.class)
            );
        }

        Set<Identifier> getAllSources() throws ReflectiveOperationException {
            Object powersValue = getPowers.invoke(component, true);
            if (!(powersValue instanceof Iterable<?> powers)) {
                return Set.of();
            }

            Set<Identifier> result = new HashSet<>();
            for (Object power : powers) {
                Object sourcesValue = getSources.invoke(component, power);
                if (!(sourcesValue instanceof Iterable<?> sources)) {
                    continue;
                }
                for (Object source : sources) {
                    if (source instanceof Identifier id) {
                        result.add(id);
                    }
                }
            }
            return result;
        }

        int removePowersInNamespace(String namespace) throws ReflectiveOperationException {
            Object powersValue = getPowers.invoke(component, true);
            if (!(powersValue instanceof Iterable<?> powers)) {
                return 0;
            }

            int removed = 0;
            for (Object power : new ArrayList<>(toList(powers))) {
                Object idValue = powerGetId.invoke(power);
                if (!(idValue instanceof Identifier id) || !namespace.equals(id.getNamespace())) {
                    continue;
                }

                Object sourcesValue = getSources.invoke(component, power);
                if (!(sourcesValue instanceof Iterable<?> sources)) {
                    continue;
                }
                for (Identifier source : identifierCopy(sources)) {
                    if (Boolean.TRUE.equals(removePower.invoke(component, power, source))) {
                        removed++;
                    }
                }
            }
            return removed;
        }

        boolean hasSource(Identifier wanted) throws ReflectiveOperationException {
            return getAllSources().contains(wanted);
        }

        boolean hasPowerPathPrefix(String namespace, String pathPrefix) throws ReflectiveOperationException {
            Object powersValue = getPowers.invoke(component, true);
            if (!(powersValue instanceof Iterable<?> powers)) {
                return false;
            }
            for (Object power : powers) {
                Object idValue = powerGetId.invoke(power);
                if (idValue instanceof Identifier id
                    && namespace.equals(id.getNamespace())
                    && id.getPath().startsWith(pathPrefix)) {
                    return true;
                }
            }
            return false;
        }

        int removePowerFromLegacySources(Set<Identifier> powerIds, Set<Identifier> selectedOrigins)
            throws ReflectiveOperationException {

            Object powersValue = getPowers.invoke(component, true);
            if (!(powersValue instanceof Iterable<?> powers)) {
                return 0;
            }

            int removed = 0;
            for (Object power : new ArrayList<>(toList(powers))) {
                Object idValue = powerGetId.invoke(power);
                if (!(idValue instanceof Identifier id) || !powerIds.contains(id)) {
                    continue;
                }

                Object sourcesValue = getSources.invoke(component, power);
                if (!(sourcesValue instanceof Iterable<?> sources)) {
                    continue;
                }
                for (Identifier source : identifierCopy(sources)) {
                    if (selectedOrigins.contains(source)) {
                        continue;
                    }
                    boolean knownLegacyNamespace = KNOWN_REMOVED_ORIGIN_NAMESPACES.contains(source.getNamespace());
                    boolean staleRegisteredOrigin = isRegisteredOrigin(source);
                    if (!knownLegacyNamespace && !staleRegisteredOrigin) {
                        continue;
                    }
                    if (Boolean.TRUE.equals(removePower.invoke(component, power, source))) {
                        removed++;
                    }
                }
            }
            return removed;
        }

        private static ArrayList<Object> toList(Iterable<?> values) {
            ArrayList<Object> result = new ArrayList<>();
            values.forEach(result::add);
            return result;
        }

        private static ArrayList<Identifier> identifierCopy(Iterable<?> values) {
            ArrayList<Identifier> result = new ArrayList<>();
            for (Object value : values) {
                if (value instanceof Identifier id) {
                    result.add(id);
                }
            }
            return result;
        }

        boolean isRegisteredOrigin(Identifier id) throws ReflectiveOperationException {
            return Boolean.TRUE.equals(originManagerContains.invoke(null, id));
        }

        int removeAllFromSource(Identifier source) throws ReflectiveOperationException {
            Object result = removeAllPowersFromSource.invoke(component, source);
            return result instanceof Number number ? number.intValue() : 0;
        }

        void sync() throws ReflectiveOperationException {
            sync.invoke(component);
        }
    }
}
