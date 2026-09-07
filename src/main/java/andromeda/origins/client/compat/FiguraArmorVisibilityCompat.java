package andromeda.origins.client.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Optional client-side bridge for custom armor renderers that bypass Figura's
 * normal vanilla armor visibility handling.
 *
 * <p>No Figura classes are linked directly. Reflection is resolved once only
 * when Figura is installed. If the expected Figura 0.1.6-era internals are not
 * available, the bridge disables itself and custom armor renders normally.</p>
 */
public final class FiguraArmorVisibilityCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger("Andromeda Origins/Figura Armor Compat");
    private static final String FIGURA_MOD_ID = "figura";

    private static volatile Access access;
    private static volatile boolean initialized;
    private static volatile boolean warned;

    private FiguraArmorVisibilityCompat() {}

    /**
     * Returns true when Figura has a loaded avatar for {@code entity}, vanilla
     * model editing is permitted, and the Figura armor group for {@code slot}
     * is hidden.
     */
    public static boolean shouldHideArmor(LivingEntity entity, EquipmentSlot slot) {
        if (entity == null || slot == null || !FabricLoader.getInstance().isModLoaded(FIGURA_MOD_ID)) {
            return false;
        }

        Access current = access;
        if (!initialized) {
            synchronized (FiguraArmorVisibilityCompat.class) {
                if (!initialized) {
                    access = current = resolve();
                    initialized = true;
                } else {
                    current = access;
                }
            }
        }

        if (current == null) {
            return false;
        }

        try {
            Object avatar = current.getAvatar.invoke(null, entity);
            if (avatar == null) {
                return false;
            }

            Object permissionPack = current.permissions.get(avatar);
            if (permissionPack == null) {
                return false;
            }

            Object permissionValue = current.permissionGet.invoke(permissionPack, current.vanillaModelEditPermission);
            if (!(permissionValue instanceof Number number) || number.intValue() < 1) {
                return false;
            }

            Object runtime = current.luaRuntime.get(avatar);
            if (runtime == null) {
                return false;
            }

            Object vanillaModel = current.vanillaModel.get(runtime);
            if (vanillaModel == null) {
                return false;
            }

            Field slotField = switch (slot) {
                case HEAD -> current.helmet;
                case CHEST -> current.chestplate;
                case LEGS -> current.leggings;
                case FEET -> current.boots;
                default -> null;
            };
            if (slotField == null) {
                return false;
            }

            Object part = slotField.get(vanillaModel);
            if (part == null) {
                return false;
            }

            Object visible = current.checkVisible.invoke(part);
            return visible instanceof Boolean bool && !bool;
        } catch (Throwable throwable) {
            disableWithWarning(throwable);
            return false;
        }
    }

    private static Access resolve() {
        try {
            Class<?> avatarManagerClass = Class.forName("org.figuramc.figura.avatar.AvatarManager");
            Class<?> avatarClass = Class.forName("org.figuramc.figura.avatar.Avatar");
            Class<?> runtimeClass = Class.forName("org.figuramc.figura.lua.FiguraLuaRuntime");
            Class<?> vanillaModelApiClass = Class.forName("org.figuramc.figura.lua.api.vanilla_model.VanillaModelAPI");
            Class<?> vanillaPartClass = Class.forName("org.figuramc.figura.lua.api.vanilla_model.VanillaPart");
            Class<?> permissionsClass = Class.forName("org.figuramc.figura.permissions.Permissions");
            Class<?> permissionPackClass = Class.forName("org.figuramc.figura.permissions.PermissionPack");

            Method getAvatar = null;
            for (Method method : avatarManagerClass.getMethods()) {
                if (method.getName().equals("getAvatar") && method.getParameterCount() == 1) {
                    getAvatar = method;
                    break;
                }
            }
            if (getAvatar == null) {
                throw new NoSuchMethodException("AvatarManager.getAvatar(entity)");
            }

            Field luaRuntime = avatarClass.getField("luaRuntime");
            Field permissions = avatarClass.getField("permissions");
            Field vanillaModel = runtimeClass.getField("vanilla_model");

            Field helmet = vanillaModelApiClass.getField("HELMET");
            Field chestplate = vanillaModelApiClass.getField("CHESTPLATE");
            Field leggings = vanillaModelApiClass.getField("LEGGINGS");
            Field boots = vanillaModelApiClass.getField("BOOTS");

            Method checkVisible = vanillaPartClass.getMethod("checkVisible");
            Field vanillaModelEdit = permissionsClass.getField("VANILLA_MODEL_EDIT");
            Object vanillaModelEditPermission = vanillaModelEdit.get(null);

            Method permissionGet = null;
            for (Method method : permissionPackClass.getMethods()) {
                if (method.getName().equals("get")
                        && method.getParameterCount() == 1
                        && method.getParameterTypes()[0] == permissionsClass) {
                    permissionGet = method;
                    break;
                }
            }
            if (permissionGet == null) {
                throw new NoSuchMethodException("PermissionPack.get(Permissions)");
            }

            LOGGER.info("Figura detected: Armor Model API armor will respect Figura vanilla_model armor visibility when compatibility is active");
            return new Access(
                    getAvatar,
                    luaRuntime,
                    permissions,
                    vanillaModel,
                    helmet,
                    chestplate,
                    leggings,
                    boots,
                    checkVisible,
                    permissionGet,
                    vanillaModelEditPermission
            );
        } catch (Throwable throwable) {
            disableWithWarning(throwable);
            return null;
        }
    }

    private static synchronized void disableWithWarning(Throwable throwable) {
        access = null;
        initialized = true;
        if (!warned) {
            warned = true;
            LOGGER.warn(
                    "Figura is installed, but Andromeda Origins could not read Figura armor visibility; Armor Model API armor will render normally",
                    throwable
            );
        }
    }

    private record Access(
            Method getAvatar,
            Field luaRuntime,
            Field permissions,
            Field vanillaModel,
            Field helmet,
            Field chestplate,
            Field leggings,
            Field boots,
            Method checkVisible,
            Method permissionGet,
            Object vanillaModelEditPermission
    ) {}
}
