package andromeda.origins.compat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small runtime optimizations for hot Apoli 2.12.x paths.
 *
 * <p>The implementation deliberately avoids hard-linking Apoli classes so the
 * project can continue to compile without an Apoli compile dependency. If an
 * expected upstream method cannot be resolved, the optimization simply falls
 * back to Apoli's original code path.</p>
 */
public final class ApoliPerformanceCompat {
    private static final String ENTITY_SET_POWER_TYPE = "io.github.apace100.apoli.power.type.EntitySetPowerType";
    private static final String POWER_HOLDER_COMPONENT = "io.github.apace100.apoli.component.PowerHolderComponent";

    private static final Map<LivingEntity, Boolean> ENTITY_SET_HOLDERS = new WeakHashMap<>();
    private static final Map<Class<?>, Field> POWERS_FIELDS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field> OWNER_FIELDS = new ConcurrentHashMap<>();

    private static volatile boolean reflectionInitialized;
    private static volatile boolean reflectionAvailable = true;
    private static Class<?> entitySetPowerClass;
    private static Method getPowerTypes;
    private static Method removeFromEntitySet;
    private static Method getPower;
    private static Method syncPowers;

    private ApoliPerformanceCompat() {
    }

    public static Collection<?> getPowerTypesSnapshot(Object component) {
        if (component == null) {
            return List.of();
        }

        try {
            Field powersField = POWERS_FIELDS.computeIfAbsent(component.getClass(), type -> {
                try {
                    Field field = type.getDeclaredField("powers");
                    field.setAccessible(true);
                    return field;
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException(exception);
                }
            });

            Object value = powersField.get(component);
            if (value instanceof Map<?, ?> map) {
                return new ArrayList<>(map.values());
            }
        } catch (Throwable ignored) {
            // Let the calling mixin fall back to Apoli's original implementation.
        }

        return List.of();
    }

    public static void refreshEntitySetHolderFromComponent(Object component) {
        if (component == null) {
            return;
        }

        try {
            Field ownerField = OWNER_FIELDS.computeIfAbsent(component.getClass(), type -> {
                try {
                    Field field = type.getDeclaredField("owner");
                    field.setAccessible(true);
                    return field;
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException(exception);
                }
            });

            Object owner = ownerField.get(component);
            if (owner instanceof LivingEntity livingOwner) {
                updateEntitySetHolder(livingOwner, getPowerTypesSnapshot(component));
            }
        } catch (Throwable ignored) {
            // Optional optimization only.
        }
    }

    /**
     * Refreshes whether a living entity currently owns any EntitySet power.
     * Called only when its power component mutates, not every tick.
     */
    public static void updateEntitySetHolder(LivingEntity owner, Collection<?> powerTypes) {
        if (owner == null || owner.getWorld().isClient()) {
            return;
        }

        boolean hasEntitySet = false;
        for (Object powerType : powerTypes) {
            if (isEntitySetPower(powerType)) {
                hasEntitySet = true;
                break;
            }
        }

        synchronized (ENTITY_SET_HOLDERS) {
            if (hasEntitySet) {
                ENTITY_SET_HOLDERS.put(owner, Boolean.TRUE);
            } else {
                ENTITY_SET_HOLDERS.remove(owner);
            }
        }
    }

    private static boolean isEntitySetPower(Object powerType) {
        if (powerType == null) {
            return false;
        }

        Class<?> type = powerType.getClass();
        while (type != null) {
            if (ENTITY_SET_POWER_TYPE.equals(type.getName())) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }

    /**
     * Replaces Apoli's world-wide EntitySet unload scan with a scan of only
     * living entities that actually hold an EntitySet power.
     *
     * @return true when the callback was fully handled and the original Apoli
     * callback can be cancelled; false means reflection was unavailable and
     * Apoli should execute its stock implementation.
     */
    public static boolean handleEntitySetUnload(Entity unloadedEntity, ServerWorld world) {
        Entity.RemovalReason reason = unloadedEntity.getRemovalReason();
        if (reason == null || !reason.shouldDestroy() || unloadedEntity instanceof PlayerEntity) {
            return true;
        }

        if (!ensureReflection()) {
            return false;
        }

        List<LivingEntity> holders;
        synchronized (ENTITY_SET_HOLDERS) {
            holders = new ArrayList<>(ENTITY_SET_HOLDERS.keySet());
        }

        if (holders.isEmpty()) {
            return true;
        }

        Map<LivingEntity, List<Object>> powersToSync = new IdentityHashMap<>();

        try {
            for (LivingEntity holder : holders) {
                if (holder == null || holder == unloadedEntity || holder.isRemoved()) {
                    synchronized (ENTITY_SET_HOLDERS) {
                        ENTITY_SET_HOLDERS.remove(holder);
                    }
                    continue;
                }

                @SuppressWarnings("unchecked")
                List<Object> entitySetPowers = (List<Object>) getPowerTypes.invoke(null, holder, entitySetPowerClass, true);

                if (entitySetPowers == null || entitySetPowers.isEmpty()) {
                    synchronized (ENTITY_SET_HOLDERS) {
                        ENTITY_SET_HOLDERS.remove(holder);
                    }
                    continue;
                }

                for (Object entitySetPower : entitySetPowers) {
                    boolean removed = (boolean) removeFromEntitySet.invoke(entitySetPower, unloadedEntity, false);
                    if (removed) {
                        Object power = getPower.invoke(entitySetPower);
                        powersToSync.computeIfAbsent(holder, ignored -> new ArrayList<>()).add(power);
                    }
                }
            }

            for (Map.Entry<LivingEntity, List<Object>> entry : powersToSync.entrySet()) {
                syncPowers.invoke(null, entry.getKey(), entry.getValue());
            }

            return true;
        } catch (Throwable ignored) {
            // Preserve compatibility over performance if an upstream signature differs.
            return false;
        }
    }


    private static boolean ensureReflection() {
        if (reflectionInitialized) {
            return reflectionAvailable;
        }

        synchronized (ApoliPerformanceCompat.class) {
            if (reflectionInitialized) {
                return reflectionAvailable;
            }

            try {
                entitySetPowerClass = Class.forName(ENTITY_SET_POWER_TYPE);
                Class<?> powerTypeClass = Class.forName("io.github.apace100.apoli.power.type.PowerType");
                Class<?> componentClass = Class.forName(POWER_HOLDER_COMPONENT);

                getPowerTypes = componentClass.getDeclaredMethod("getPowerTypes", Entity.class, Class.class, boolean.class);
                removeFromEntitySet = entitySetPowerClass.getMethod("remove", Entity.class, boolean.class);
                getPower = powerTypeClass.getMethod("getPower");
                syncPowers = componentClass.getDeclaredMethod("syncPowers", Entity.class, Collection.class);

                getPowerTypes.setAccessible(true);
                syncPowers.setAccessible(true);
            } catch (Throwable ignored) {
                reflectionAvailable = false;
            } finally {
                reflectionInitialized = true;
            }
        }

        return reflectionAvailable;
    }
}
