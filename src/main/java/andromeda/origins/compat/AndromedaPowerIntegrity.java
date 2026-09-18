package andromeda.origins.compat;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * General post-Origin-sync integrity pass for Andromeda-owned powers.
 *
 * <p>Apoli persists concrete MultiplePower children. That is useful for resources/cooldowns, but it
 * also means a player can carry an older child set after a datapack update. This reconciler repairs
 * the complete current power closure for each selected Andromeda Origin and then verifies every
 * currently-owned Andromeda MultiplePower, including temporary helper powers. Missing children are
 * added under the parent's existing source without resetting already-present resources.</p>
 *
 * <p>For powers owned by a selected Andromeda Origin source, obsolete members are removed only when
 * they are no longer part of that Origin's current power closure. No powers belonging to other mods
 * or generic temporary sources are removed by that origin-source cleanup.</p>
 */
public final class AndromedaPowerIntegrity {

    private static final Logger LOGGER = LoggerFactory.getLogger("Andromeda Origins/Power Integrity");
    private static final String NAMESPACE = "andromeda_origins";
    private static final Identifier ORIGIN_LAYER_SYNC_PHASE = Identifier.of("origins", "origin_layers");
    private static final Identifier NEREID_PHASE = Identifier.of(NAMESPACE, "nereid_breathing_migration");
    private static final Identifier INTEGRITY_PHASE = Identifier.of(NAMESPACE, "power_integrity");

    private static final Identifier SOURCE_DEBUFF = Identifier.of("minecraft", "debuff");
    private static final Identifier SOURCE_CCONTROL = Identifier.of("minecraft", "ccontrol");
    private static final Identifier SOURCE_BUFF = Identifier.of("minecraft", "buff");

    // Order matters: Petrified itself supplies Restrained/Silenced/Pacified, so reconcile it first.
    private static final Set<String> ORIGIN_COMMAND_TAGS = Set.of(
        "arachne", "faerie", "fenrkin", "gorgon", "human", "lichling", "manticore",
        "nereid", "satyr", "selkie", "siren", "veilborn", "wyverian"
    );

    private static final List<SharedStatusSpec> SHARED_STATUSES = List.of(
        new SharedStatusSpec("common/petrified", SOURCE_CCONTROL, List.of("gorgon/helper/ophidianed")),
        new SharedStatusSpec("common/restrained", SOURCE_CCONTROL, List.of(
            "common/petrified", "arachne/helper/webbed", "gorgon/helper/constricted"
        )),
        new SharedStatusSpec("common/silenced", SOURCE_CCONTROL, List.of(
            "common/petrified", "siren/helper/infatuated", "veilborn/helper/watered", "selkie/helper/groundweak"
        )),
        new SharedStatusSpec("common/pacified", SOURCE_CCONTROL, List.of(
            "common/petrified", "siren/helper/infatuated"
        )),
        new SharedStatusSpec("common/wet", SOURCE_DEBUFF, List.of(
            "nereid/helper/nereidwet", "selkie/helper/selkiewet"
        )),
        new SharedStatusSpec("common/unstoppable", SOURCE_BUFF, List.of(
            "fenrkin/helper/unstoppable_anim", "manticore/helper/unstoppable_anim"
        ))
    );

    private AndromedaPowerIntegrity() {}

    public record Result(int powersAdded, int powersRemoved, int resourcesCorrected, boolean successful) {
        public boolean changed() { return powersAdded > 0 || powersRemoved > 0 || resourcesCorrected > 0; }
    }

    private record SharedStatusSpec(String path, Identifier source, List<String> providers) {
        Identifier powerId() { return Identifier.of(NAMESPACE, path); }
        Identifier resourceId() { return Identifier.of(NAMESPACE, path + "_sources"); }
    }

    public static void register() {
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.addPhaseOrdering(ORIGIN_LAYER_SYNC_PHASE, INTEGRITY_PHASE);
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.addPhaseOrdering(NEREID_PHASE, INTEGRITY_PHASE);
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register(INTEGRITY_PHASE, (player, joined) -> reconcile(player));
        // CCA/Origins normally copies powers across respawn, but run the same cheap one-shot
        // verification on the replacement player entity so missing child membership cannot survive
        // a death/respawn boundary. Defer onto the server queue so component copying has finished.
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
            newPlayer.getServer().execute(() -> reconcile(newPlayer)));
    }

    public static Result reconcile(ServerPlayerEntity player) {
        try {
            ReflectionBridge bridge = ReflectionBridge.create(player);
            if (bridge == null) {
                return new Result(0, 0, 0, false);
            }

            Collection<?> selectedOrigins = getSelectedOrigins(player);
            if (selectedOrigins.isEmpty()) {
                // Origin data is not trustworthy yet; fail closed rather than guessing.
                return new Result(0, 0, 0, true);
            }

            int added = 0;
            int removed = 0;
            int resourcesCorrected = 0;
            int tagCorrections = 0;
            Set<Identifier> selectedAndromedaOrigins = new HashSet<>();

            // First reconcile each selected Andromeda Origin's exact current power closure.
            for (Object origin : selectedOrigins) {
                Identifier originId = bridge.originId(origin);
                if (originId == null || !NAMESPACE.equals(originId.getNamespace())) {
                    continue;
                }
                selectedAndromedaOrigins.add(originId);

                Collection<?> roots = bridge.originPowers(origin);
                Map<Identifier, Object> expected = new HashMap<>();
                for (Object root : roots) {
                    collectPowerClosure(bridge, root, expected);
                }

                // Add current roots/children that legacy players are missing.
                for (Object root : roots) {
                    added += ensureClosureOwned(bridge, root, originId, new HashSet<>());
                }

                // Remove only obsolete members owned specifically by this selected Origin source.
                List<Object> currentFromSource = new ArrayList<>(bridge.powersFromSource(originId));
                for (Object owned : currentFromSource) {
                    Identifier id = bridge.powerId(owned);
                    if (id != null && !expected.containsKey(id) && bridge.removePower(owned, originId)) {
                        removed++;
                    }
                }
            }

            // Then repair child membership for temporary/helper MultiplePowers under all sources.
            // This is what prevents an old helper from retaining its parent while missing the timer
            // or cleanup child that makes the effect expire.
            List<Object> snapshot = new ArrayList<>(bridge.allPowers());
            for (Object power : snapshot) {
                Identifier id = bridge.powerId(power);
                if (id == null || !NAMESPACE.equals(id.getNamespace())) {
                    continue;
                }

                Collection<?> children = bridge.subPowers(power);
                if (children.isEmpty()) {
                    continue;
                }

                for (Identifier source : bridge.sources(power)) {
                    for (Object child : children) {
                        if (!bridge.hasPower(child, source) && bridge.addPower(child, source)) {
                            added++;
                        }
                    }
                }
            }

            // Re-derive shared temporary states from the helpers that actually provide them.
            // This closes the other major persistence failure mode: a provider can disappear while
            // its shared source counter remains non-zero, leaving Wet/CC/Unstoppable stuck.
            for (SharedStatusSpec status : SHARED_STATUSES) {
                StatusRepair repaired = reconcileSharedStatus(bridge, status);
                added += repaired.added();
                removed += repaired.removed();
                resourcesCorrected += repaired.resourceCorrected() ? 1 : 0;
            }

            // Undetectable predates the shared source-counter system, so derive it from its actual
            // active state resources/helpers instead of trusting common/undetectable_sources.
            StatusRepair undetectable = reconcileUndetectable(bridge);
            added += undetectable.added();
            removed += undetectable.removed();
            resourcesCorrected += undetectable.resourceCorrected() ? 1 : 0;

            // Origin command tags are compatibility state used by Figura and a few legacy hooks.
            // entity_action_chosen is not guaranteed to rerun after every datapack power reload,
            // so derive these tags from the selected Origin instead of trusting the old callback.
            if (!selectedAndromedaOrigins.isEmpty()) {
                tagCorrections = reconcileOriginTags(player, selectedAndromedaOrigins);
            }

            if (added > 0 || removed > 0 || resourcesCorrected > 0) {
                bridge.sync();
            }
            if (added > 0 || removed > 0 || resourcesCorrected > 0 || tagCorrections > 0) {
                LOGGER.info(
                    "Reconciled Andromeda power/state integrity for {} (added {}, removed {}, resources corrected {}, tags corrected {}).",
                    player.getGameProfile().getName(), added, removed, resourcesCorrected, tagCorrections
                );
            }

            return new Result(added, removed, resourcesCorrected, true);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.error("Could not safely reconcile Andromeda power membership for {}.",
                player.getGameProfile().getName(), unwrap(exception));
            return new Result(0, 0, 0, false);
        }
    }

    private static int reconcileOriginTags(ServerPlayerEntity player, Set<Identifier> selectedOrigins) {
        Set<String> expected = new HashSet<>();
        for (Identifier originId : selectedOrigins) {
            String path = originId.getPath();
            if (path.startsWith("champion_")) {
                path = path.substring("champion_".length());
            }
            if ("humanity".equals(path)) {
                path = "human";
            }
            if (ORIGIN_COMMAND_TAGS.contains(path)) {
                expected.add(path);
            }
        }

        int corrected = 0;
        for (String tag : ORIGIN_COMMAND_TAGS) {
            boolean has = player.getCommandTags().contains(tag);
            boolean shouldHave = expected.contains(tag);
            if (shouldHave && !has) {
                player.addCommandTag(tag);
                corrected++;
            } else if (!shouldHave && has) {
                player.removeCommandTag(tag);
                corrected++;
            }
        }
        return corrected;
    }

    private record StatusRepair(int added, int removed, boolean resourceCorrected) {}

    private static StatusRepair reconcileSharedStatus(ReflectionBridge bridge, SharedStatusSpec spec)
        throws ReflectiveOperationException {
        Object status = bridge.resolvePower(spec.powerId());
        Object counter = bridge.resolvePower(spec.resourceId());
        if (status == null || counter == null) {
            return new StatusRepair(0, 0, false);
        }

        int providers = 0;
        for (String providerPath : spec.providers()) {
            Object provider = bridge.resolvePower(Identifier.of(NAMESPACE, providerPath));
            if (provider != null && bridge.hasPower(provider)) {
                providers++;
            }
        }

        int added = 0;
        int removed = 0;
        boolean corrected = false;

        if (providers <= 0) {
            if (bridge.hasPower(status, spec.source()) && bridge.removePower(status, spec.source())) {
                removed++;
            }
            // Removing the shared power is not enough: VariableIntPowerType#setValue does not
            // execute the resource min_action, and a stale positive source count would contaminate
            // the next application. Normalize the counter explicitly while the real provider set is empty.
            corrected = bridge.setResourceValueIfDifferent(counter, 0);
            return new StatusRepair(added, removed, corrected);
        }

        if (!bridge.hasPower(status, spec.source()) && bridge.addPower(status, spec.source())) {
            added++;
        }
        corrected = bridge.setResourceValueIfDifferent(counter, providers);
        return new StatusRepair(added, removed, corrected);
    }

    private static StatusRepair reconcileUndetectable(ReflectionBridge bridge)
        throws ReflectiveOperationException {
        Object undetectable = bridge.resolvePower(Identifier.of(NAMESPACE, "common/undetectable"));
        Object counter = bridge.resolvePower(Identifier.of(NAMESPACE, "common/undetectable_sources"));
        if (undetectable == null) {
            return new StatusRepair(0, 0, false);
        }

        boolean desired = bridge.resourceValue(Identifier.of(NAMESPACE, "faerie/passives_stealthstate")) > 0
            || bridge.resourceValue(Identifier.of(NAMESPACE, "champion/faerie_passives_stealthstate")) > 0
            || bridge.resourceValue(Identifier.of(NAMESPACE, "champion/manticore_passives_bloodriftstate")) > 0
            || bridge.hasPower(Identifier.of(NAMESPACE, "veilborn/helper/veilpower"));

        int added = 0;
        int removed = 0;
        boolean corrected = false;
        if (desired) {
            if (!bridge.hasPower(undetectable, SOURCE_BUFF) && bridge.addPower(undetectable, SOURCE_BUFF)) {
                added++;
            }
            if (counter != null) {
                corrected = bridge.setResourceValueIfDifferent(counter, 1);
            }
        } else {
            if (bridge.hasPower(undetectable, SOURCE_BUFF) && bridge.removePower(undetectable, SOURCE_BUFF)) {
                removed++;
            }
            if (counter != null) {
                corrected = bridge.setResourceValueIfDifferent(counter, 0);
            }
        }
        return new StatusRepair(added, removed, corrected);
    }

    private static int ensureClosureOwned(
        ReflectionBridge bridge,
        Object power,
        Identifier source,
        Set<Identifier> visited
    ) throws ReflectiveOperationException {
        Identifier id = bridge.powerId(power);
        if (id == null || !visited.add(id)) {
            return 0;
        }

        int added = 0;
        if (!bridge.hasPower(power, source) && bridge.addPower(power, source)) {
            added++;
        }

        // addPower(parent) normally recurses, but explicitly verify children in case the parent was
        // already owned and Apoli returned early without walking newly-added child definitions.
        for (Object child : bridge.subPowers(power)) {
            added += ensureClosureOwned(bridge, child, source, visited);
        }
        return added;
    }

    private static void collectPowerClosure(
        ReflectionBridge bridge,
        Object power,
        Map<Identifier, Object> result
    ) throws ReflectiveOperationException {
        Identifier id = bridge.powerId(power);
        if (id == null || result.putIfAbsent(id, power) != null) {
            return;
        }
        for (Object child : bridge.subPowers(power)) {
            collectPowerClosure(bridge, child, result);
        }
    }

    private static Collection<?> getSelectedOrigins(ServerPlayerEntity player) throws ReflectiveOperationException {
        Class<?> modComponentsClass = Class.forName("io.github.apace100.origins.registry.ModComponents");
        Field originKeyField = modComponentsClass.getField("ORIGIN");
        Object originKey = originKeyField.get(null);
        Method getComponent = findCompatibleSingleArgMethod(originKeyField.getType(), "get", player);
        Object originComponent = getComponent.invoke(originKey, player);
        if (originComponent == null) {
            return List.of();
        }
        Method getOrigins = originComponent.getClass().getMethod("getOrigins");
        Object value = getOrigins.invoke(originComponent);
        if (!(value instanceof Map<?, ?> map)) {
            return List.of();
        }
        return map.values().stream().filter(v -> v != null).toList();
    }

    private static Method findCompatibleSingleArgMethod(Class<?> ownerClass, String name, Object argument)
        throws NoSuchMethodException {
        for (Method method : ownerClass.getMethods()) {
            if (method.getName().equals(name)
                && method.getParameterCount() == 1
                && method.getParameterTypes()[0].isInstance(argument)) {
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
        private final Method hasPower;
        private final Method hasAnyPower;
        private final Method addPower;
        private final Method removePower;
        private final Method getSources;
        private final Method getPowersFromSource;
        private final Method getPowers;
        private final Method getPowerType;
        private final Method powerManagerGetNullable;
        private final Class<?> variableIntPowerType;
        private final Method variableGetValue;
        private final Method variableSetValue;
        private final Method powerGetId;
        private final Method originGetId;
        private final Method originGetPowers;
        private final Method sync;

        private ReflectionBridge(
            Object component,
            Method hasPower,
            Method hasAnyPower,
            Method addPower,
            Method removePower,
            Method getSources,
            Method getPowersFromSource,
            Method getPowers,
            Method getPowerType,
            Method powerManagerGetNullable,
            Class<?> variableIntPowerType,
            Method variableGetValue,
            Method variableSetValue,
            Method powerGetId,
            Method originGetId,
            Method originGetPowers,
            Method sync
        ) {
            this.component = component;
            this.hasPower = hasPower;
            this.hasAnyPower = hasAnyPower;
            this.addPower = addPower;
            this.removePower = removePower;
            this.getSources = getSources;
            this.getPowersFromSource = getPowersFromSource;
            this.getPowers = getPowers;
            this.getPowerType = getPowerType;
            this.powerManagerGetNullable = powerManagerGetNullable;
            this.variableIntPowerType = variableIntPowerType;
            this.variableGetValue = variableGetValue;
            this.variableSetValue = variableSetValue;
            this.powerGetId = powerGetId;
            this.originGetId = originGetId;
            this.originGetPowers = originGetPowers;
            this.sync = sync;
        }

        static ReflectionBridge create(ServerPlayerEntity player) throws ReflectiveOperationException {
            Class<?> componentClass = Class.forName("io.github.apace100.apoli.component.PowerHolderComponent");
            Class<?> powerClass = Class.forName("io.github.apace100.apoli.power.Power");
            Class<?> powerManagerClass = Class.forName("io.github.apace100.apoli.power.PowerManager");
            Class<?> variableIntPowerType = Class.forName("io.github.apace100.apoli.power.type.VariableIntPowerType");
            Class<?> originClass = Class.forName("io.github.apace100.origins.origin.Origin");
            Method getNullable = componentClass.getMethod("getNullable", Entity.class);
            Object component = getNullable.invoke(null, player);
            if (component == null) {
                return null;
            }
            return new ReflectionBridge(
                component,
                componentClass.getMethod("hasPower", powerClass, Identifier.class),
                componentClass.getMethod("hasPower", powerClass),
                componentClass.getMethod("addPower", powerClass, Identifier.class),
                componentClass.getMethod("removePower", powerClass, Identifier.class),
                componentClass.getMethod("getSources", powerClass),
                componentClass.getMethod("getPowersFromSource", Identifier.class),
                componentClass.getMethod("getPowers", boolean.class),
                componentClass.getMethod("getPowerType", powerClass),
                powerManagerClass.getMethod("getNullable", Identifier.class),
                variableIntPowerType,
                variableIntPowerType.getMethod("getValue"),
                variableIntPowerType.getMethod("setValue", int.class),
                powerClass.getMethod("getId"),
                originClass.getMethod("getId"),
                originClass.getMethod("getPowers"),
                componentClass.getMethod("sync")
            );
        }

        Identifier originId(Object origin) throws ReflectiveOperationException {
            Object value = originGetId.invoke(origin);
            return value instanceof Identifier id ? id : null;
        }

        Collection<?> originPowers(Object origin) throws ReflectiveOperationException {
            Object value = originGetPowers.invoke(origin);
            return value instanceof Collection<?> collection ? collection : List.of();
        }

        Identifier powerId(Object power) throws ReflectiveOperationException {
            Object value = powerGetId.invoke(power);
            return value instanceof Identifier id ? id : null;
        }

        boolean hasPower(Object power, Identifier source) throws ReflectiveOperationException {
            return Boolean.TRUE.equals(hasPower.invoke(component, power, source));
        }

        boolean hasPower(Object power) throws ReflectiveOperationException {
            return Boolean.TRUE.equals(hasAnyPower.invoke(component, power));
        }

        boolean hasPower(Identifier powerId) throws ReflectiveOperationException {
            Object power = resolvePower(powerId);
            return power != null && hasPower(power);
        }

        Object resolvePower(Identifier id) throws ReflectiveOperationException {
            return powerManagerGetNullable.invoke(null, id);
        }

        int resourceValue(Identifier id) throws ReflectiveOperationException {
            Object power = resolvePower(id);
            if (power == null || !hasPower(power)) {
                return 0;
            }
            Object type = getPowerType.invoke(component, power);
            if (type == null || !variableIntPowerType.isInstance(type)) {
                return 0;
            }
            return ((Number) variableGetValue.invoke(type)).intValue();
        }

        boolean setResourceValueIfDifferent(Object power, int value) throws ReflectiveOperationException {
            if (power == null || !hasPower(power)) {
                return false;
            }
            Object type = getPowerType.invoke(component, power);
            if (type == null || !variableIntPowerType.isInstance(type)) {
                return false;
            }
            int current = ((Number) variableGetValue.invoke(type)).intValue();
            if (current == value) {
                return false;
            }
            variableSetValue.invoke(type, value);
            return true;
        }

        boolean addPower(Object power, Identifier source) throws ReflectiveOperationException {
            return Boolean.TRUE.equals(addPower.invoke(component, power, source));
        }

        boolean removePower(Object power, Identifier source) throws ReflectiveOperationException {
            return Boolean.TRUE.equals(removePower.invoke(component, power, source));
        }

        Collection<?> powersFromSource(Identifier source) throws ReflectiveOperationException {
            Object value = getPowersFromSource.invoke(component, source);
            return value instanceof Collection<?> collection ? collection : List.of();
        }

        Collection<?> allPowers() throws ReflectiveOperationException {
            Object value = getPowers.invoke(component, true);
            return value instanceof Collection<?> collection ? collection : List.of();
        }

        Set<Identifier> sources(Object power) throws ReflectiveOperationException {
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

        Collection<?> subPowers(Object power) throws ReflectiveOperationException {
            try {
                Method method = power.getClass().getMethod("getSubPowers");
                Object value = method.invoke(power);
                return value instanceof Collection<?> collection ? collection : List.of();
            } catch (NoSuchMethodException ignored) {
                return List.of();
            }
        }

        void sync() throws ReflectiveOperationException {
            sync.invoke(component);
        }
    }
}
