package andromeda.origins.compat;

import andromeda.origins.AndromedaOrigins;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.DefaultAttributeRegistry;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Rebuilds the raw player attributes Andromeda Origins relies on, then asks Apoli's currently
 * granted attribute powers to re-apply themselves. This deliberately does not re-select the
 * Origin, so selection callbacks, ability cooldowns, resources, and active timers are preserved.
 *
 * The raw values are restored from Minecraft's PLAYER default-attribute container instead of a
 * hand-maintained table. The effective result is therefore:
 *
 *     clean player base + the player's currently granted Origin/Champion powers
 *
 * This means a repaired Arachne, Lichling, Champion, etc. returns to that Origin's effective stats
 * rather than being left at vanilla-player values.
 */
public final class OriginAttributeRepair {

    private static final Logger LOGGER = LoggerFactory.getLogger("Andromeda Origins/Attribute Repair");

    // These are the vanilla attributes Andromeda's current Origin powers directly modify.
    // Other vanilla/modded base attributes are intentionally left alone so the repair command does
    // not erase unrelated systems which may legitimately own their base values.
    private static final List<RegistryEntry<EntityAttribute>> ANDROMEDA_MANAGED_ATTRIBUTES = List.of(
        EntityAttributes.GENERIC_MAX_HEALTH,
        EntityAttributes.GENERIC_MOVEMENT_SPEED,
        EntityAttributes.GENERIC_SCALE,
        EntityAttributes.GENERIC_STEP_HEIGHT,
        EntityAttributes.GENERIC_ARMOR,
        EntityAttributes.GENERIC_ARMOR_TOUGHNESS,
        EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,
        EntityAttributes.GENERIC_ATTACK_SPEED
    );

    private OriginAttributeRepair() {}

    public record Result(
        int baseAttributesReset,
        int staleAndromedaModifiersRemoved,
        int attributePowersReapplied,
        boolean apoliRebuilt
    ) {}

    public static Result repairAttributes(ServerPlayerEntity player) {
        final ReflectionBridge bridge;
        final List<Object> attributePowerTypes;

        try {
            bridge = ReflectionBridge.create(player);
            if (bridge == null) {
                LOGGER.warn("Could not access the Apoli power component for {}; attribute repair was skipped rather than leaving vanilla-only stats.",
                    player.getGameProfile().getName());
                return new Result(0, 0, 0, false);
            }
            attributePowerTypes = bridge.attributePowerTypes();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            // Fail closed: resetting bases without being able to re-apply the selected Origin's
            // powers would turn the player into a vanilla stat line, which is exactly what this
            // command is meant to avoid.
            LOGGER.error("Could not inspect Apoli powers for {}; attribute repair was skipped.",
                player.getGameProfile().getName(), exception);
            return new Result(0, 0, 0, false);
        }

        try {
            // Remove modifiers owned by the currently granted Apoli AttributeModifying powers.
            for (Object powerType : attributePowerTypes) {
                bridge.removeModifiers(powerType, player);
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            // Some modifiers may already have been removed before a later power failed. Restore every
            // active attribute power before returning so a failed repair attempt does not itself leave
            // the player with a partial stat line.
            restoreActivePowerModifiersBestEffort(bridge, attributePowerTypes, player);
            LOGGER.error("Could not remove current Apoli attribute modifiers for {}; raw bases were not changed and active modifiers were restored best-effort.",
                player.getGameProfile().getName(), exception);
            return new Result(0, 0, 0, false);
        }

        // Catch orphaned persistent modifiers from a previously selected Andromeda Origin. Those
        // are not visible through the current power component anymore, but their namespaced IDs
        // still identify them as Andromeda-owned. Equipment and other mods use their own namespaces
        // and are left untouched.
        int staleModifiersRemoved = removeAndromedaNamespaceModifiers(player);
        int basesReset = resetManagedBaseAttributes(player);
        int reapplied = 0;
        boolean reapplyFailed = false;

        for (Object powerType : attributePowerTypes) {
            try {
                if (!bridge.isActive(powerType)) {
                    continue;
                }

                bridge.reapply(powerType, player);
                reapplied++;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                reapplyFailed = true;
                LOGGER.error("Could not re-apply one Apoli attribute power for {} after resetting raw bases.",
                    player.getGameProfile().getName(), exception);
            }
        }

        try {
            bridge.sync();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            reapplyFailed = true;
            LOGGER.warn("Could not sync Apoli power data after repairing {}.",
                player.getGameProfile().getName(), exception);
        }

        // The admin repair command has historically restored the player to full health. Do that
        // only after the Origin's max-health modifiers have been reconstructed.
        float maxHealth = player.getMaxHealth();
        if (Float.isFinite(maxHealth) && maxHealth > 0.0F) {
            player.setHealth(maxHealth);
        }

        boolean rebuilt = !reapplyFailed;
        LOGGER.info("Repaired {} raw Andromeda attributes for {}; removed {} stale Andromeda modifiers and re-applied {} active Apoli attribute powers (apoliRebuilt={}).",
            basesReset, player.getGameProfile().getName(), staleModifiersRemoved, reapplied, rebuilt);

        return new Result(basesReset, staleModifiersRemoved, reapplied, rebuilt);
    }


    private static void restoreActivePowerModifiersBestEffort(
        ReflectionBridge bridge,
        List<Object> attributePowerTypes,
        ServerPlayerEntity player
    ) {
        for (Object powerType : attributePowerTypes) {
            try {
                if (bridge.isActive(powerType)) {
                    bridge.reapply(powerType, player);
                }
            } catch (ReflectiveOperationException | RuntimeException restoreException) {
                LOGGER.warn("Could not restore one Apoli attribute power for {} while aborting a failed repair.",
                    player.getGameProfile().getName(), restoreException);
            }
        }
    }

    private static int removeAndromedaNamespaceModifiers(ServerPlayerEntity player) {
        int removed = 0;

        for (RegistryEntry<EntityAttribute> attribute : ANDROMEDA_MANAGED_ATTRIBUTES) {
            EntityAttributeInstance instance = player.getAttributeInstance(attribute);
            if (instance == null) {
                continue;
            }

            // Copy first because removeModifier mutates the backing collection.
            List<EntityAttributeModifier> modifiers = new ArrayList<>(instance.getModifiers());
            for (EntityAttributeModifier modifier : modifiers) {
                if (AndromedaOrigins.MOD_ID.equals(modifier.id().getNamespace())) {
                    instance.removeModifier(modifier.id());
                    removed++;
                }
            }
        }

        return removed;
    }

    private static int resetManagedBaseAttributes(ServerPlayerEntity player) {
        DefaultAttributeContainer defaults = DefaultAttributeRegistry.get(EntityType.PLAYER);
        int reset = 0;

        for (RegistryEntry<EntityAttribute> attribute : ANDROMEDA_MANAGED_ATTRIBUTES) {
            if (!defaults.has(attribute)) {
                continue;
            }

            EntityAttributeInstance instance = player.getAttributeInstance(attribute);
            if (instance == null) {
                continue;
            }

            instance.setBaseValue(defaults.getBaseValue(attribute));
            reset++;
        }

        return reset;
    }

    /**
     * The project intentionally does not hard-link Apoli Java classes in its build. Reflection
     * keeps that boundary while still using Apoli's own modifier add/remove methods at runtime.
     */
    private static final class ReflectionBridge {
        private static final String CONDITIONED_ATTRIBUTE_CLASS =
            "io.github.apace100.apoli.power.type.ConditionedAttributePowerType";
        private static final String LAVA_VISION_CLASS =
            "io.github.apace100.apoli.power.type.LavaVisionPowerType";

        private final Object component;
        private final Class<?> attributeModifyingClass;
        private final Method getPowerTypes;
        private final Method isActive;
        private final Method removeModifiers;
        private final Method addPersistentModifiers;
        private final Method addTemporaryModifiers;
        private final Method sync;

        private ReflectionBridge(
            Object component,
            Class<?> attributeModifyingClass,
            Method getPowerTypes,
            Method isActive,
            Method removeModifiers,
            Method addPersistentModifiers,
            Method addTemporaryModifiers,
            Method sync
        ) {
            this.component = component;
            this.attributeModifyingClass = attributeModifyingClass;
            this.getPowerTypes = getPowerTypes;
            this.isActive = isActive;
            this.removeModifiers = removeModifiers;
            this.addPersistentModifiers = addPersistentModifiers;
            this.addTemporaryModifiers = addTemporaryModifiers;
            this.sync = sync;
        }

        static ReflectionBridge create(ServerPlayerEntity player) throws ReflectiveOperationException {
            Class<?> componentClass = Class.forName("io.github.apace100.apoli.component.PowerHolderComponent");
            Class<?> powerTypeClass = Class.forName("io.github.apace100.apoli.power.type.PowerType");
            Class<?> attributeModifyingClass = Class.forName("io.github.apace100.apoli.power.type.AttributeModifying");

            Method getNullable = componentClass.getMethod("getNullable", Entity.class);
            Object component = getNullable.invoke(null, player);
            if (component == null) {
                return null;
            }

            return new ReflectionBridge(
                component,
                attributeModifyingClass,
                componentClass.getMethod("getPowerTypes"),
                powerTypeClass.getMethod("isActive"),
                attributeModifyingClass.getMethod("removeModifiers", LivingEntity.class),
                attributeModifyingClass.getMethod("addPersistentModifiers", LivingEntity.class),
                attributeModifyingClass.getMethod("addTemporaryModifiers", LivingEntity.class),
                componentClass.getMethod("sync")
            );
        }

        List<Object> attributePowerTypes() throws ReflectiveOperationException {
            Object value = getPowerTypes.invoke(component);
            if (!(value instanceof List<?> powers)) {
                return List.of();
            }

            List<Object> result = new ArrayList<>();
            for (Object powerType : powers) {
                if (powerType != null && attributeModifyingClass.isInstance(powerType)) {
                    result.add(powerType);
                }
            }
            return result;
        }

        boolean isActive(Object powerType) throws ReflectiveOperationException {
            return Boolean.TRUE.equals(isActive.invoke(powerType));
        }

        void removeModifiers(Object powerType, LivingEntity entity) throws ReflectiveOperationException {
            removeModifiers.invoke(powerType, entity);
        }

        void reapply(Object powerType, LivingEntity entity) throws ReflectiveOperationException {
            if (usesTemporaryModifiers(powerType)) {
                addTemporaryModifiers.invoke(powerType, entity);
            } else {
                addPersistentModifiers.invoke(powerType, entity);
            }
        }

        private boolean usesTemporaryModifiers(Object powerType) {
            for (Class<?> type = powerType.getClass(); type != null; type = type.getSuperclass()) {
                String name = type.getName();
                if (CONDITIONED_ATTRIBUTE_CLASS.equals(name) || LAVA_VISION_CLASS.equals(name)) {
                    return true;
                }
            }
            return false;
        }

        void sync() throws ReflectiveOperationException {
            sync.invoke(component);
        }
    }
}
