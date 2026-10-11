package andromeda.origins.client.compat




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
    val LOGGER: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger("Andromeda Origins/Figura Armor Compat")
    private const val FIGURA_MOD_ID: kotlin.String = "figura"

    @kotlin.concurrent.Volatile
    private var access: FiguraArmorVisibilityCompat.Access? = null
    @kotlin.concurrent.Volatile
    var initialized: kotlin.Boolean = false
    @kotlin.concurrent.Volatile
    var warned: kotlin.Boolean = false

     /**
 * Returns true when Figura has a loaded avatar for `entity`, vanilla
 * model editing is permitted, and the Figura armor group for `slot`
 * is hidden.
 */
    fun shouldHideArmor(entity: net.minecraft.world.entity.LivingEntity?, slot: net.minecraft.world.entity.EquipmentSlot?): kotlin.Boolean {
        if (entity == null || slot == null || !andromeda.origins.platform.Platform.isModLoaded(FiguraArmorVisibilityCompat.FIGURA_MOD_ID)) {
            return false
        }

        var current: FiguraArmorVisibilityCompat.Access? = FiguraArmorVisibilityCompat.access
        if (!FiguraArmorVisibilityCompat.initialized) {
            kotlin.synchronized(FiguraArmorVisibilityCompat::class.java) {if (!FiguraArmorVisibilityCompat.initialized) {
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
            val avatar: kotlin.Any? = current.getAvatar.invoke(null, entity)
            if (avatar == null) {
                return false
            }

            val permissionPack: kotlin.Any? = current.permissions.get(avatar)
            if (permissionPack == null) {
                return false
            }

            val permissionValue: kotlin.Any? = current.permissionGet.invoke(permissionPack, current.vanillaModelEditPermission)
            val number = permissionValue as? kotlin.Number
            if (number == null || permissionValue.toInt() < 1) {
                return false
            }

            val runtime: kotlin.Any? = current.luaRuntime.get(avatar)
            if (runtime == null) {
                return false
            }

            val vanillaModel: kotlin.Any? = current.vanillaModel.get(runtime)
            if (vanillaModel == null) {
                return false
            }

            val slotField: java.lang.reflect.Field? = when (slot){net.minecraft.world.entity.EquipmentSlot.HEAD -> current.helmet
                net.minecraft.world.entity.EquipmentSlot.CHEST -> current.chestplate
                net.minecraft.world.entity.EquipmentSlot.LEGS -> current.leggings
                net.minecraft.world.entity.EquipmentSlot.FEET -> current.boots
                else -> null}
            if (slotField == null) {
                return false
            }

            val part: kotlin.Any? = slotField.get(vanillaModel)
            if (part == null) {
                return false
            }

            val visible: kotlin.Any? = current.checkVisible.invoke(part)
            val bool = visible as? kotlin.Boolean
            return bool != null && !visible
        }catch (throwable: kotlin.Throwable) {
            FiguraArmorVisibilityCompat.disableWithWarning(throwable)
            return false
        }
    }

    private fun resolve(): FiguraArmorVisibilityCompat.Access? {
        try {
            val avatarManagerClass: java.lang.Class<*> = java.lang.Class.forName("org.figuramc.figura.avatar.AvatarManager")
            val avatarClass: java.lang.Class<*> = java.lang.Class.forName("org.figuramc.figura.avatar.Avatar")
            val runtimeClass: java.lang.Class<*> = java.lang.Class.forName("org.figuramc.figura.lua.FiguraLuaRuntime")
            val vanillaModelApiClass: java.lang.Class<*> = java.lang.Class.forName("org.figuramc.figura.lua.api.vanilla_model.VanillaModelAPI")
            val vanillaPartClass: java.lang.Class<*> = java.lang.Class.forName("org.figuramc.figura.lua.api.vanilla_model.VanillaPart")
            val permissionsClass: java.lang.Class<*> = java.lang.Class.forName("org.figuramc.figura.permissions.Permissions")
            val permissionPackClass: java.lang.Class<*> = java.lang.Class.forName("org.figuramc.figura.permissions.PermissionPack")

            val getAvatar = avatarManagerClass.methods.firstOrNull {
                it.name == "getAvatar" && it.parameterCount == 1 && it.parameterTypes[0].isAssignableFrom(net.minecraft.world.entity.LivingEntity::class.java)
            } ?: throw java.lang.NoSuchMethodException("AvatarManager.getAvatar(entity)")

            val luaRuntime: java.lang.reflect.Field = avatarClass.getField("luaRuntime")
            val permissions: java.lang.reflect.Field = avatarClass.getField("permissions")
            val vanillaModel: java.lang.reflect.Field = runtimeClass.getField("vanilla_model")

            val helmet: java.lang.reflect.Field = vanillaModelApiClass.getField("HELMET")
            val chestplate: java.lang.reflect.Field = vanillaModelApiClass.getField("CHESTPLATE")
            val leggings: java.lang.reflect.Field = vanillaModelApiClass.getField("LEGGINGS")
            val boots: java.lang.reflect.Field = vanillaModelApiClass.getField("BOOTS")

            val checkVisible: java.lang.reflect.Method = vanillaPartClass.getMethod("checkVisible")
            val vanillaModelEdit: java.lang.reflect.Field = permissionsClass.getField("VANILLA_MODEL_EDIT")
            val vanillaModelEditPermission: kotlin.Any? = vanillaModelEdit.get(null)

            val permissionGet = permissionPackClass.methods.firstOrNull {
                it.name == "get" && it.parameterCount == 1 && it.parameterTypes[0] == permissionsClass
            } ?: throw java.lang.NoSuchMethodException("PermissionPack.get(Permissions)")

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
        }catch (throwable: kotlin.Throwable) {
            FiguraArmorVisibilityCompat.disableWithWarning(throwable)
            return null
        }
    }

    @kotlin.jvm.Synchronized private fun disableWithWarning(throwable: kotlin.Throwable?) {
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


    private data class Access (val getAvatar: java.lang.reflect.Method,
    val luaRuntime: java.lang.reflect.Field,
    val permissions: java.lang.reflect.Field,
    val vanillaModel: java.lang.reflect.Field,
    val helmet: java.lang.reflect.Field,
    val chestplate: java.lang.reflect.Field,
    val leggings: java.lang.reflect.Field,
    val boots: java.lang.reflect.Field,
    val checkVisible: java.lang.reflect.Method,
    val permissionGet: java.lang.reflect.Method,
    val vanillaModelEditPermission: kotlin.Any?
    ) {
    }
}
