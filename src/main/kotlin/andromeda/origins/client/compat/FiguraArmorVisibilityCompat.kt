package andromeda.origins.client.compat

import andromeda.origins.platform.Platform
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.concurrent.Volatile
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import org.slf4j.Logger
import org.slf4j.LoggerFactory




 /**
 * Optional client-side bridge for custom armor renderers that bypass Figura's
 * normal vanilla armor visibility handling.
 *
 *
 * No Figura classes are linked directly. Reflection is resolved once only
 * when Figura is installed. If the expected Figura 0.1.6-era internals are not
 * available, the bridge disables itself and custom armor renders normally.
 */
object FiguraArmorVisibilityCompat {
    val LOGGER: Logger = LoggerFactory.getLogger("Andromeda Origins/Figura Armor Compat")
    private const val FIGURA_MOD_ID: String = "figura"

    @Volatile
    private var access: FiguraArmorVisibilityCompat.Access? = null
    @Volatile
    var initialized: Boolean = false
    @Volatile
    var warned: Boolean = false

     /**
 * Returns true when Figura has a loaded avatar for `entity`, vanilla
 * model editing is permitted, and the Figura armor group for `slot`
 * is hidden.
 */
    fun shouldHideArmor(entity: LivingEntity?, slot: EquipmentSlot?): Boolean {
        if (entity == null || slot == null || !Platform.isModLoaded(FiguraArmorVisibilityCompat.FIGURA_MOD_ID)) {
            return false
        }

        var current: FiguraArmorVisibilityCompat.Access? = FiguraArmorVisibilityCompat.access
        if (!FiguraArmorVisibilityCompat.initialized) {
            synchronized(FiguraArmorVisibilityCompat::class.java) {if (!FiguraArmorVisibilityCompat.initialized) {
                current = FiguraArmorVisibilityCompat.resolve()
                FiguraArmorVisibilityCompat.access = current
                FiguraArmorVisibilityCompat.initialized = true
            } else {
                current = FiguraArmorVisibilityCompat.access
            }
            }
        }

        if (current == null) {
            return false
        }

        try {
            val avatar: Any? = current.getAvatar.invoke(null, entity)
            if (avatar == null) {
                return false
            }

            val permissionPack: Any? = current.permissions.get(avatar)
            if (permissionPack == null) {
                return false
            }

            val permissionValue: Any? = current.permissionGet.invoke(permissionPack, current.vanillaModelEditPermission)
            val number = permissionValue as? Number
            if (number == null || permissionValue.toInt() < 1) {
                return false
            }

            val runtime: Any? = current.luaRuntime.get(avatar)
            if (runtime == null) {
                return false
            }

            val vanillaModel: Any? = current.vanillaModel.get(runtime)
            if (vanillaModel == null) {
                return false
            }

            val slotField: Field? = when (slot){EquipmentSlot.HEAD -> current.helmet
                EquipmentSlot.CHEST -> current.chestplate
                EquipmentSlot.LEGS -> current.leggings
                EquipmentSlot.FEET -> current.boots
                else -> null}
            if (slotField == null) {
                return false
            }

            val part: Any? = slotField.get(vanillaModel)
            if (part == null) {
                return false
            }

            val visible: Any? = current.checkVisible.invoke(part)
            val bool = visible as? Boolean
            return bool != null && !visible
        }catch (throwable: Throwable) {
            FiguraArmorVisibilityCompat.disableWithWarning(throwable)
            return false
        }
    }

    private fun resolve(): FiguraArmorVisibilityCompat.Access? {
        try {
            val avatarManagerClass: Class<*> = Class.forName("org.figuramc.figura.avatar.AvatarManager")
            val avatarClass: Class<*> = Class.forName("org.figuramc.figura.avatar.Avatar")
            val runtimeClass: Class<*> = Class.forName("org.figuramc.figura.lua.FiguraLuaRuntime")
            val vanillaModelApiClass: Class<*> = Class.forName("org.figuramc.figura.lua.api.vanilla_model.VanillaModelAPI")
            val vanillaPartClass: Class<*> = Class.forName("org.figuramc.figura.lua.api.vanilla_model.VanillaPart")
            val permissionsClass: Class<*> = Class.forName("org.figuramc.figura.permissions.Permissions")
            val permissionPackClass: Class<*> = Class.forName("org.figuramc.figura.permissions.PermissionPack")

            val getAvatar = avatarManagerClass.methods.firstOrNull {
                it.name == "getAvatar" && it.parameterCount == 1 && it.parameterTypes[0].isAssignableFrom(LivingEntity::class.java)
            } ?: throw NoSuchMethodException("AvatarManager.getAvatar(entity)")

            val luaRuntime: Field = avatarClass.getField("luaRuntime")
            val permissions: Field = avatarClass.getField("permissions")
            val vanillaModel: Field = runtimeClass.getField("vanilla_model")

            val helmet: Field = vanillaModelApiClass.getField("HELMET")
            val chestplate: Field = vanillaModelApiClass.getField("CHESTPLATE")
            val leggings: Field = vanillaModelApiClass.getField("LEGGINGS")
            val boots: Field = vanillaModelApiClass.getField("BOOTS")

            val checkVisible: Method = vanillaPartClass.getMethod("checkVisible")
            val vanillaModelEdit: Field = permissionsClass.getField("VANILLA_MODEL_EDIT")
            val vanillaModelEditPermission: Any? = vanillaModelEdit.get(null)

            val permissionGet = permissionPackClass.methods.firstOrNull {
                it.name == "get" && it.parameterCount == 1 && it.parameterTypes[0] == permissionsClass
            } ?: throw NoSuchMethodException("PermissionPack.get(Permissions)")

            FiguraArmorVisibilityCompat.LOGGER.info("Figura detected: Armor Model API armor will respect Figura vanilla_model armor visibility when compatibility is active")
            return FiguraArmorVisibilityCompat.Access(
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
            )
        }catch (throwable: Throwable) {
            FiguraArmorVisibilityCompat.disableWithWarning(throwable)
            return null
        }
    }

    @Synchronized private fun disableWithWarning(throwable: Throwable?) {
        FiguraArmorVisibilityCompat.access = null
        FiguraArmorVisibilityCompat.initialized = true
        if (!FiguraArmorVisibilityCompat.warned) {
            FiguraArmorVisibilityCompat.warned = true
            FiguraArmorVisibilityCompat.LOGGER.warn(
            "Figura is installed, but Andromeda Origins could not read Figura armor visibility; Armor Model API armor will render normally",
            throwable
            )
        }
    }


    private data class Access (val getAvatar: Method,
    val luaRuntime: Field,
    val permissions: Field,
    val vanillaModel: Field,
    val helmet: Field,
    val chestplate: Field,
    val leggings: Field,
    val boots: Field,
    val checkVisible: Method,
    val permissionGet: Method,
    val vanillaModelEditPermission: Any?
    ) {
    }
}
