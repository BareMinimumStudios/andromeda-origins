package andromeda.origins.compat

import andromeda.origins.compat.MortalResolveManager
import java.util.HashMap
import andromeda.origins.compat.MortalResolveEffects


 /**
 * Optional compatibility bridge for CartoonishVillain's Incapacitated.
 *
 * This module intentionally uses reflection instead of a hard compile/runtime dependency. That
 * keeps Andromeda Origins usable without Incapacitated while still allowing us to ask the mod's
 * own player-data object whether the player is actually downed before calling its revival API.
 */
object IncapacitatedCompat {
    val LOGGER: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger("Andromeda Origins/Incapacitated Compat")
    private const val MOD_ID: kotlin.String = "incapacitated"
    var reflectionWarningLogged: kotlin.Boolean = false

    val FIELDS: kotlin.collections.MutableMap<kotlin.String, java.lang.reflect.Field> = HashMap<kotlin.String, java.lang.reflect.Field>()
    val METHODS: kotlin.collections.MutableMap<kotlin.String, java.lang.reflect.Method> = HashMap<kotlin.String, java.lang.reflect.Method>()

    @kotlin.Throws(java.lang.ReflectiveOperationException::class) private fun field(owner: kotlin.String, name: kotlin.String): java.lang.reflect.Field {
        val key: kotlin.String = owner + "#" + name
        val cached: java.lang.reflect.Field? = IncapacitatedCompat.FIELDS.get(key)
        if (cached != null) return cached
        val resolved: java.lang.reflect.Field = java.lang.Class.forName(owner).getField(name)
        IncapacitatedCompat.FIELDS.put(key, resolved)
        return resolved
    }

    @kotlin.Throws(java.lang.ReflectiveOperationException::class) private fun method(owner: java.lang.Class<*>, name: kotlin.String, vararg parameters: java.lang.Class<*>?): java.lang.reflect.Method {
        val key: kotlin.String = owner.getName() + "#" + name + parameters.contentToString()
        val cached: java.lang.reflect.Method? = IncapacitatedCompat.METHODS.get(key)
        if (cached != null) return cached
        val resolved: java.lang.reflect.Method = owner.getMethod(name, *parameters)
        IncapacitatedCompat.METHODS.put(key, resolved)
        return resolved
    }

    @kotlin.Throws(java.lang.ReflectiveOperationException::class) private fun platformMethod(owner: java.lang.Class<*>, name: kotlin.String, count: kotlin.Int): java.lang.reflect.Method {
        val key: kotlin.String = owner.getName() + "#" + name + "/" + count
        val cached: java.lang.reflect.Method? = IncapacitatedCompat.METHODS.get(key)
        if (cached != null) return cached
        val resolved: java.lang.reflect.Method = java.util.Arrays.stream<java.lang.reflect.Method>(owner.getMethods())
        .filter { candidate: java.lang.reflect.Method -> candidate.getName() == name && candidate.getParameterCount() == count}
        .findFirst().orElseThrow<java.lang.NoSuchMethodException>(java.util.function.Supplier {java.lang.NoSuchMethodException(name)})
        IncapacitatedCompat.METHODS.put(key, resolved)
        return resolved
    }

    fun isLoaded(): kotlin.Boolean {
        return andromeda.origins.platform.Platform.isModLoaded(IncapacitatedCompat.MOD_ID)
    }

     /**
 * @return true only when Incapacitated is loaded and its own component reports the player downed.
 */
    fun isDowned(player: net.minecraft.server.level.ServerPlayer?): kotlin.Boolean {
        if (!IncapacitatedCompat.isLoaded()) {
            return false
        }

        try {
            val platform: kotlin.Any = IncapacitatedCompat.getPlatform()
            val playerData: kotlin.Any? = IncapacitatedCompat.getPlayerData(platform, player)
            if (playerData == null) {
                return false
            }

            val isIncapacitated: java.lang.reflect.Method = IncapacitatedCompat.method(playerData.javaClass, "isIncapacitated")
            return java.lang.Boolean.TRUE == isIncapacitated.invoke(playerData)
        }catch (exception: java.lang.ReflectiveOperationException) {
            if (!IncapacitatedCompat.reflectionWarningLogged) {
                IncapacitatedCompat.reflectionWarningLogged = true
                IncapacitatedCompat.LOGGER.warn("Could not read Incapacitated player state. Safe revive will no-op rather than forcing a revive.", exception)
            }
            return false
        }catch (exception: java.lang.RuntimeException) {
            if (!IncapacitatedCompat.reflectionWarningLogged) {
                IncapacitatedCompat.reflectionWarningLogged = true
                IncapacitatedCompat.LOGGER.warn("Could not read Incapacitated player state. Safe revive will no-op rather than forcing a revive.", exception)
            }
            return false
        }
    }

    @kotlin.Throws(java.lang.ReflectiveOperationException::class) private fun getPlatform(): kotlin.Any {
        val platformField: java.lang.reflect.Field = IncapacitatedCompat.field("com.cartoonishvillain.incapacitated.platform.Services", "PLATFORM")
        return platformField.get(null)
    }

    @kotlin.Throws(java.lang.ReflectiveOperationException::class) private fun getPlayerData(platform: kotlin.Any, player: net.minecraft.server.level.ServerPlayer?): kotlin.Any? {
        val getPlayerData: java.lang.reflect.Method = IncapacitatedCompat.platformMethod(platform.javaClass, "getPlayerData", 1)
        return getPlayerData.invoke(platform, player)
    }

     /**
 * Incapacitated keeps the last damage and pre-hit health in runtime component fields. They are
 * not cleared by its normal revive method. Resetting them after an Andromeda-triggered revive
 * prevents an interrupted/cancelled damage sequence from leaking stale overkill data into the
 * next hit.
 */
    private fun resetDamageTracking(player: net.minecraft.server.level.ServerPlayer) {
        if (!IncapacitatedCompat.isLoaded()) {
            return
        }

        try {
            val platform: kotlin.Any = IncapacitatedCompat.getPlatform()
            val playerData: kotlin.Any? = IncapacitatedCompat.getPlayerData(platform, player)
            if (playerData == null) {
                return
            }

            IncapacitatedCompat.method(playerData.javaClass, "setLastDmgTaken", kotlin.Float::class.javaPrimitiveType).invoke(playerData, 0.0f)
            IncapacitatedCompat.method(playerData.javaClass, "setLastHealthBeforeDamage", kotlin.Float::class.javaPrimitiveType)
            .invoke(playerData, kotlin.math.max(1.0f, player.getHealth()))

            IncapacitatedCompat.writePlayerData(platform, player, playerData)
        }catch (exception: java.lang.ReflectiveOperationException) {
            if (!IncapacitatedCompat.reflectionWarningLogged) {
                IncapacitatedCompat.reflectionWarningLogged = true
                IncapacitatedCompat.LOGGER.warn("Could not reset Incapacitated transient damage tracking.", exception)
            }
        }catch (exception: java.lang.RuntimeException) {
            if (!IncapacitatedCompat.reflectionWarningLogged) {
                IncapacitatedCompat.reflectionWarningLogged = true
                IncapacitatedCompat.LOGGER.warn("Could not reset Incapacitated transient damage tracking.", exception)
            }
        }
    }

    @kotlin.Throws(java.lang.ReflectiveOperationException::class) private fun getConfigData(): kotlin.Any {
        val configField: java.lang.reflect.Field = IncapacitatedCompat.field("com.cartoonishvillain.incapacitated.Incapacitated", "configData")
        return configField.get(null)
    }

    @kotlin.Throws(java.lang.ReflectiveOperationException::class) private fun writePlayerData(platform: kotlin.Any, player: net.minecraft.server.level.ServerPlayer, playerData: kotlin.Any?) {
        val writePlayerData: java.lang.reflect.Method = IncapacitatedCompat.platformMethod(platform.javaClass, "writePlayerData", 2)
        writePlayerData.invoke(platform, player, playerData)
    }

     /**
 * Restores Incapacitated's normal post-death counters after Mortal Resolve temporarily forces
 * downsUntilDeath to -1. This prevents the final-stand sentinel from leaking into later lives.
 */
    fun restorePostMortalResolveState(player: net.minecraft.server.level.ServerPlayer) {
        if (!IncapacitatedCompat.isLoaded()) {
            return
        }

        try {
            val platform: kotlin.Any = IncapacitatedCompat.getPlatform()
            val playerData: kotlin.Any? = IncapacitatedCompat.getPlayerData(platform, player)
            val configData: kotlin.Any? = IncapacitatedCompat.getConfigData()
            if (playerData == null || configData == null) {
                return
            }

            val downCounter: kotlin.Int = (IncapacitatedCompat.method(configData.javaClass, "getDownCounter").invoke(configData) as kotlin.Number).toInt()
            val downTicks: kotlin.Int = (IncapacitatedCompat.method(configData.javaClass, "getDownTicks").invoke(configData) as kotlin.Number).toInt()

            IncapacitatedCompat.method(playerData.javaClass, "setIncapacitated", kotlin.Boolean::class.javaPrimitiveType).invoke(playerData, false)
            IncapacitatedCompat.method(playerData.javaClass, "setDownsUntilDeath", kotlin.Int::class.javaPrimitiveType).invoke(playerData, kotlin.math.max(0, downCounter))
            IncapacitatedCompat.method(playerData.javaClass, "setTicksUntilDeath", kotlin.Int::class.javaPrimitiveType).invoke(playerData, kotlin.math.max(1, downTicks))
            IncapacitatedCompat.writePlayerData(platform, player, playerData)
            IncapacitatedCompat.resetDamageTracking(player)
        }catch (exception: java.lang.ReflectiveOperationException) {
            if (!IncapacitatedCompat.reflectionWarningLogged) {
                IncapacitatedCompat.reflectionWarningLogged = true
                IncapacitatedCompat.LOGGER.warn("Could not restore Incapacitated state after Mortal Resolve death.", exception)
            }
        }catch (exception: java.lang.RuntimeException) {
            if (!IncapacitatedCompat.reflectionWarningLogged) {
                IncapacitatedCompat.reflectionWarningLogged = true
                IncapacitatedCompat.LOGGER.warn("Could not restore Incapacitated state after Mortal Resolve death.", exception)
            }
        }
    }

     /**
 * Self-heals the stale -1 counter produced by older Andromeda versions when Incapacitated is
 * configured for UnlimitedDowns. Unlimited downs still requires a non-negative stored counter
 * in Incapacitated's death gate; it merely stops decrementing that counter.
 */
    fun repairUnlimitedDownCounter(player: net.minecraft.server.level.ServerPlayer) {
        if (!IncapacitatedCompat.isLoaded()) {
            return
        }

        try {
            val configData: kotlin.Any = IncapacitatedCompat.getConfigData()
            val unlimited: kotlin.Boolean = java.lang.Boolean.TRUE == IncapacitatedCompat.method(configData.javaClass, "isUnlimitedDowns").invoke(configData)
            if (!unlimited) {
                return
            }

            val platform: kotlin.Any = IncapacitatedCompat.getPlatform()
            val playerData: kotlin.Any? = IncapacitatedCompat.getPlayerData(platform, player)
            if (playerData == null) {
                return
            }

            val current: kotlin.Int = (IncapacitatedCompat.method(playerData.javaClass, "getDownsUntilDeath").invoke(playerData) as kotlin.Number).toInt()
            if (current >= 0) {
                return
            }

            val downCounter: kotlin.Int = (IncapacitatedCompat.method(configData.javaClass, "getDownCounter").invoke(configData) as kotlin.Number).toInt()
            IncapacitatedCompat.method(playerData.javaClass, "setDownsUntilDeath", kotlin.Int::class.javaPrimitiveType).invoke(playerData, kotlin.math.max(0, downCounter))
            IncapacitatedCompat.writePlayerData(platform, player, playerData)
            IncapacitatedCompat.resetDamageTracking(player)
            IncapacitatedCompat.LOGGER.info("Repaired stale Incapacitated unlimited-down counter for {}.", player.getGameProfile().getName())
        }catch (exception: java.lang.ReflectiveOperationException) {
            if (!IncapacitatedCompat.reflectionWarningLogged) {
                IncapacitatedCompat.reflectionWarningLogged = true
                IncapacitatedCompat.LOGGER.warn("Could not repair Incapacitated unlimited-down counter.", exception)
            }
        }catch (exception: java.lang.RuntimeException) {
            if (!IncapacitatedCompat.reflectionWarningLogged) {
                IncapacitatedCompat.reflectionWarningLogged = true
                IncapacitatedCompat.LOGGER.warn("Could not repair Incapacitated unlimited-down counter.", exception)
            }
        }
    }

     /**
 * Revives only if Incapacitated says the player is currently downed. This avoids the upstream
 * setDowned false command resetting a healthy player's health to the configured revive health.
 */
    fun safeRevive(player: net.minecraft.server.level.ServerPlayer): kotlin.Boolean {
        if (!IncapacitatedCompat.isDowned(player)) {
            return false
        }

        try {
            // Brigadier can defer nested commands until the current command finishes. Calling
            // the upstream API synchronously keeps subsequent heals/counter writes in order.
            val owner = Class.forName("com.cartoonishvillain.incapacitated.events.AbstractedIncapacitation")
            method(owner, "revive", net.minecraft.world.entity.player.Player::class.java,
                net.minecraft.world.entity.player.Player::class.java).invoke(null, player, null)
            if (isDowned(player)) return false
            resetDamageTracking(player)
            return true
        } catch (exception: ReflectiveOperationException) {
            LOGGER.warn("Could not revive {} through Incapacitated's API.", player.gameProfile.name, exception)
            return false
        } catch (exception: RuntimeException) {
            LOGGER.warn("Could not revive {} through Incapacitated's API.", player.gameProfile.name, exception)
            return false
        }
    }

     /**
 * Starts Humanity's Mortal Resolve only from a real Incapacitated downed state.
 */
    fun beginMortalResolve(player: net.minecraft.server.level.ServerPlayer): kotlin.Boolean {
        if (MortalResolveManager.isActive(player) || !IncapacitatedCompat.safeRevive(player)) {
            return false
        }

        MortalResolveManager.start(player)
                // Mortal Resolve is a final stand from the moment the revive succeeds. This also makes
        // direct kill paths final; the Fabric fatal-damage event reasserts the value for ordinary
        // damage in case another Incapacitated mechanic changes the down counter mid-stand.
        IncapacitatedCompat.prepareMortalResolveDeath(player)

                // Amplifier 3 is level IV in vanilla status-effect numbering. The direct effect API is
        // equivalent to /effect give ... 60 3 false, without a Brigadier round trip.
        IncapacitatedCompat.applyMortalResolveCombatEffects(player)

        MortalResolveEffects.playActivation(player)
        return true
    }


     /**
 * Champion Humanity version of Mortal Resolve: safely revives and grants the same one-minute
 * combat effects without arming Mortal Resolve's forced-death/finality manager.
 */
    fun beginChampionMortalResolve(player: net.minecraft.server.level.ServerPlayer): kotlin.Boolean {
        if (MortalResolveManager.isActive(player)
        || MortalResolveManager.isChampionActive(player)
        || !IncapacitatedCompat.safeRevive(player)) {
            return false
        }

        MortalResolveManager.startChampion(player)

        IncapacitatedCompat.applyMortalResolveCombatEffects(player)

        MortalResolveEffects.playActivation(player)
        return true
    }

    private fun applyMortalResolveCombatEffects(player: net.minecraft.server.level.ServerPlayer) {
        player.addEffect(net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 20 * 60, 3, false, true, true))
        player.addEffect(net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 20 * 60, 3, false, true, true))
    }

     /**
 * Marks the next death as final for Incapacitated. The mod's own death hook checks its
 * downs-until-death value, so setting it below zero immediately before death prevents a second
 * down without replacing or hard-depending on Incapacitated's classes. Andromeda explicitly
 * restores the configured counter after the final death so this temporary sentinel cannot leak
 * into a later life.
 */
    fun prepareMortalResolveDeath(player: net.minecraft.server.level.ServerPlayer) {
        if (!IncapacitatedCompat.isLoaded()) {
            return
        }

        try {
            val platform: kotlin.Any = IncapacitatedCompat.getPlatform()
            val playerData: kotlin.Any? = IncapacitatedCompat.getPlayerData(platform, player)
            if (playerData == null) {
                return
            }

            IncapacitatedCompat.method(playerData.javaClass, "setIncapacitated", kotlin.Boolean::class.javaPrimitiveType).invoke(playerData, false)
            IncapacitatedCompat.method(playerData.javaClass, "setDownsUntilDeath", kotlin.Int::class.javaPrimitiveType).invoke(playerData, -1)

            IncapacitatedCompat.writePlayerData(platform, player, playerData)

            IncapacitatedCompat.resetDamageTracking(player)
        }catch (exception: java.lang.ReflectiveOperationException) {
            if (!IncapacitatedCompat.reflectionWarningLogged) {
                IncapacitatedCompat.reflectionWarningLogged = true
                IncapacitatedCompat.LOGGER.warn("Could not prepare Mortal Resolve's final death for Incapacitated.", exception)
            }
        }catch (exception: java.lang.RuntimeException) {
            if (!IncapacitatedCompat.reflectionWarningLogged) {
                IncapacitatedCompat.reflectionWarningLogged = true
                IncapacitatedCompat.LOGGER.warn("Could not prepare Mortal Resolve's final death for Incapacitated.", exception)
            }
        }
    }

     /**
 * Ends Mortal Resolve after one minute. Earlier/final death handling is owned by MortalResolveManager.
 */
    fun forceMortalResolveDeath(player: net.minecraft.server.level.ServerPlayer) {
        MortalResolveEffects.playExpiry(player)
        IncapacitatedCompat.prepareMortalResolveDeath(player)
        player.kill()

                // The active tag must survive through the actual death check so Humanity's own
        // prevent-death mechanics cannot become eligible again at expiry. Once the kill
        // has succeeded, clear the runtime/tag state immediately rather than waiting for
        // the next server tick or respawn callback.
        if (!player.isAlive()) {
            MortalResolveManager.stop(player)
        }
    }

}
