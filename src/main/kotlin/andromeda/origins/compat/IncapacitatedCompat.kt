package andromeda.origins.compat

import andromeda.origins.compat.MortalResolveEffects
import andromeda.origins.compat.MortalResolveManager
import andromeda.origins.platform.Platform
import java.lang.Boolean as JavaBoolean
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.Arrays
import java.util.HashMap
import java.util.function.Supplier
import kotlin.math.max
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Player
import org.slf4j.Logger
import org.slf4j.LoggerFactory


 /**
 * Optional compatibility bridge for CartoonishVillain's Incapacitated.
 *
 * This module intentionally uses reflection instead of a hard compile/runtime dependency. That
 * keeps Andromeda Origins usable without Incapacitated while still allowing us to ask the mod's
 * own player-data object whether the player is actually downed before calling its revival API.
 */
object IncapacitatedCompat {
    val LOGGER: Logger = LoggerFactory.getLogger("Andromeda Origins/Incapacitated Compat")
    private const val MOD_ID: String = "incapacitated"
    var reflectionWarningLogged: Boolean = false

    val FIELDS: MutableMap<String, Field> = HashMap<String, Field>()
    val METHODS: MutableMap<String, Method> = HashMap<String, Method>()

    @Throws(ReflectiveOperationException::class) private fun field(owner: String, name: String): Field {
        val key: String = owner + "#" + name
        val cached: Field? = IncapacitatedCompat.FIELDS.get(key)
        if (cached != null) return cached
        val resolved: Field = Class.forName(owner).getField(name)
        IncapacitatedCompat.FIELDS.put(key, resolved)
        return resolved
    }

    @Throws(ReflectiveOperationException::class) private fun method(owner: Class<*>, name: String, vararg parameters: Class<*>?): Method {
        val key: String = owner.getName() + "#" + name + parameters.contentToString()
        val cached: Method? = IncapacitatedCompat.METHODS.get(key)
        if (cached != null) return cached
        val resolved: Method = owner.getMethod(name, *parameters)
        IncapacitatedCompat.METHODS.put(key, resolved)
        return resolved
    }

    @Throws(ReflectiveOperationException::class) private fun platformMethod(owner: Class<*>, name: String, count: Int): Method {
        val key: String = owner.getName() + "#" + name + "/" + count
        val cached: Method? = IncapacitatedCompat.METHODS.get(key)
        if (cached != null) return cached
        val resolved: Method = Arrays.stream<Method>(owner.getMethods())
        .filter { candidate: Method -> candidate.getName() == name && candidate.getParameterCount() == count}
        .findFirst().orElseThrow<NoSuchMethodException>(Supplier {NoSuchMethodException(name)})
        IncapacitatedCompat.METHODS.put(key, resolved)
        return resolved
    }

    fun isLoaded(): Boolean {
        return Platform.isModLoaded(IncapacitatedCompat.MOD_ID)
    }

     /**
 * @return true only when Incapacitated is loaded and its own component reports the player downed.
 */
    fun isDowned(player: ServerPlayer?): Boolean {
        if (!IncapacitatedCompat.isLoaded()) {
            return false
        }

        try {
            val platform: Any = IncapacitatedCompat.getPlatform()
            val playerData: Any? = IncapacitatedCompat.getPlayerData(platform, player)
            if (playerData == null) {
                return false
            }

            val isIncapacitated: Method = IncapacitatedCompat.method(playerData.javaClass, "isIncapacitated")
            return JavaBoolean.TRUE == isIncapacitated.invoke(playerData)
        }catch (exception: ReflectiveOperationException) {
            if (!IncapacitatedCompat.reflectionWarningLogged) {
                IncapacitatedCompat.reflectionWarningLogged = true
                IncapacitatedCompat.LOGGER.warn("Could not read Incapacitated player state. Safe revive will no-op rather than forcing a revive.", exception)
            }
            return false
        }catch (exception: RuntimeException) {
            if (!IncapacitatedCompat.reflectionWarningLogged) {
                IncapacitatedCompat.reflectionWarningLogged = true
                IncapacitatedCompat.LOGGER.warn("Could not read Incapacitated player state. Safe revive will no-op rather than forcing a revive.", exception)
            }
            return false
        }
    }

    @Throws(ReflectiveOperationException::class) private fun getPlatform(): Any {
        val platformField: Field = IncapacitatedCompat.field("com.cartoonishvillain.incapacitated.platform.Services", "PLATFORM")
        return platformField.get(null)
    }

    @Throws(ReflectiveOperationException::class) private fun getPlayerData(platform: Any, player: ServerPlayer?): Any? {
        val getPlayerData: Method = IncapacitatedCompat.platformMethod(platform.javaClass, "getPlayerData", 1)
        return getPlayerData.invoke(platform, player)
    }

     /**
 * Incapacitated keeps the last damage and pre-hit health in runtime component fields. They are
 * not cleared by its normal revive method. Resetting them after an Andromeda-triggered revive
 * prevents an interrupted/cancelled damage sequence from leaking stale overkill data into the
 * next hit.
 */
    private fun resetDamageTracking(player: ServerPlayer) {
        if (!IncapacitatedCompat.isLoaded()) {
            return
        }

        try {
            val platform: Any = IncapacitatedCompat.getPlatform()
            val playerData: Any? = IncapacitatedCompat.getPlayerData(platform, player)
            if (playerData == null) {
                return
            }

            IncapacitatedCompat.method(playerData.javaClass, "setLastDmgTaken", Float::class.javaPrimitiveType).invoke(playerData, 0.0f)
            IncapacitatedCompat.method(playerData.javaClass, "setLastHealthBeforeDamage", Float::class.javaPrimitiveType)
            .invoke(playerData, max(1.0f, player.getHealth()))

            IncapacitatedCompat.writePlayerData(platform, player, playerData)
        }catch (exception: ReflectiveOperationException) {
            if (!IncapacitatedCompat.reflectionWarningLogged) {
                IncapacitatedCompat.reflectionWarningLogged = true
                IncapacitatedCompat.LOGGER.warn("Could not reset Incapacitated transient damage tracking.", exception)
            }
        }catch (exception: RuntimeException) {
            if (!IncapacitatedCompat.reflectionWarningLogged) {
                IncapacitatedCompat.reflectionWarningLogged = true
                IncapacitatedCompat.LOGGER.warn("Could not reset Incapacitated transient damage tracking.", exception)
            }
        }
    }

    @Throws(ReflectiveOperationException::class) private fun getConfigData(): Any {
        val configField: Field = IncapacitatedCompat.field("com.cartoonishvillain.incapacitated.Incapacitated", "configData")
        return configField.get(null)
    }

    @Throws(ReflectiveOperationException::class) private fun writePlayerData(platform: Any, player: ServerPlayer, playerData: Any?) {
        val writePlayerData: Method = IncapacitatedCompat.platformMethod(platform.javaClass, "writePlayerData", 2)
        writePlayerData.invoke(platform, player, playerData)
    }

     /**
 * Restores Incapacitated's normal post-death counters after Mortal Resolve temporarily forces
 * downsUntilDeath to -1. This prevents the final-stand sentinel from leaking into later lives.
 */
    fun restorePostMortalResolveState(player: ServerPlayer) {
        if (!IncapacitatedCompat.isLoaded()) {
            return
        }

        try {
            val platform: Any = IncapacitatedCompat.getPlatform()
            val playerData: Any? = IncapacitatedCompat.getPlayerData(platform, player)
            val configData: Any? = IncapacitatedCompat.getConfigData()
            if (playerData == null || configData == null) {
                return
            }

            val downCounter: Int = (IncapacitatedCompat.method(configData.javaClass, "getDownCounter").invoke(configData) as Number).toInt()
            val downTicks: Int = (IncapacitatedCompat.method(configData.javaClass, "getDownTicks").invoke(configData) as Number).toInt()

            IncapacitatedCompat.method(playerData.javaClass, "setIncapacitated", Boolean::class.javaPrimitiveType).invoke(playerData, false)
            IncapacitatedCompat.method(playerData.javaClass, "setDownsUntilDeath", Int::class.javaPrimitiveType).invoke(playerData, max(0, downCounter))
            IncapacitatedCompat.method(playerData.javaClass, "setTicksUntilDeath", Int::class.javaPrimitiveType).invoke(playerData, max(1, downTicks))
            IncapacitatedCompat.writePlayerData(platform, player, playerData)
            IncapacitatedCompat.resetDamageTracking(player)
        }catch (exception: ReflectiveOperationException) {
            if (!IncapacitatedCompat.reflectionWarningLogged) {
                IncapacitatedCompat.reflectionWarningLogged = true
                IncapacitatedCompat.LOGGER.warn("Could not restore Incapacitated state after Mortal Resolve death.", exception)
            }
        }catch (exception: RuntimeException) {
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
    fun repairUnlimitedDownCounter(player: ServerPlayer) {
        if (!IncapacitatedCompat.isLoaded()) {
            return
        }

        try {
            val configData: Any = IncapacitatedCompat.getConfigData()
            val unlimited: Boolean = JavaBoolean.TRUE == IncapacitatedCompat.method(configData.javaClass, "isUnlimitedDowns").invoke(configData)
            if (!unlimited) {
                return
            }

            val platform: Any = IncapacitatedCompat.getPlatform()
            val playerData: Any? = IncapacitatedCompat.getPlayerData(platform, player)
            if (playerData == null) {
                return
            }

            val current: Int = (IncapacitatedCompat.method(playerData.javaClass, "getDownsUntilDeath").invoke(playerData) as Number).toInt()
            if (current >= 0) {
                return
            }

            val downCounter: Int = (IncapacitatedCompat.method(configData.javaClass, "getDownCounter").invoke(configData) as Number).toInt()
            IncapacitatedCompat.method(playerData.javaClass, "setDownsUntilDeath", Int::class.javaPrimitiveType).invoke(playerData, max(0, downCounter))
            IncapacitatedCompat.writePlayerData(platform, player, playerData)
            IncapacitatedCompat.resetDamageTracking(player)
            IncapacitatedCompat.LOGGER.info("Repaired stale Incapacitated unlimited-down counter for {}.", player.getGameProfile().getName())
        }catch (exception: ReflectiveOperationException) {
            if (!IncapacitatedCompat.reflectionWarningLogged) {
                IncapacitatedCompat.reflectionWarningLogged = true
                IncapacitatedCompat.LOGGER.warn("Could not repair Incapacitated unlimited-down counter.", exception)
            }
        }catch (exception: RuntimeException) {
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
    fun safeRevive(player: ServerPlayer): Boolean {
        if (!IncapacitatedCompat.isDowned(player)) {
            return false
        }

        try {
            // Brigadier can defer nested commands until the current command finishes. Calling
            // the upstream API synchronously keeps subsequent heals/counter writes in order.
            val owner = Class.forName("com.cartoonishvillain.incapacitated.events.AbstractedIncapacitation")
            method(owner, "revive", Player::class.java,
                Player::class.java).invoke(null, player, null)
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
    fun beginMortalResolve(player: ServerPlayer): Boolean {
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
    fun beginChampionMortalResolve(player: ServerPlayer): Boolean {
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

    private fun applyMortalResolveCombatEffects(player: ServerPlayer) {
        player.addEffect(MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 60, 3, false, true, true))
        player.addEffect(MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 60, 3, false, true, true))
    }

     /**
 * Marks the next death as final for Incapacitated. The mod's own death hook checks its
 * downs-until-death value, so setting it below zero immediately before death prevents a second
 * down without replacing or hard-depending on Incapacitated's classes. Andromeda explicitly
 * restores the configured counter after the final death so this temporary sentinel cannot leak
 * into a later life.
 */
    fun prepareMortalResolveDeath(player: ServerPlayer) {
        if (!IncapacitatedCompat.isLoaded()) {
            return
        }

        try {
            val platform: Any = IncapacitatedCompat.getPlatform()
            val playerData: Any? = IncapacitatedCompat.getPlayerData(platform, player)
            if (playerData == null) {
                return
            }

            IncapacitatedCompat.method(playerData.javaClass, "setIncapacitated", Boolean::class.javaPrimitiveType).invoke(playerData, false)
            IncapacitatedCompat.method(playerData.javaClass, "setDownsUntilDeath", Int::class.javaPrimitiveType).invoke(playerData, -1)

            IncapacitatedCompat.writePlayerData(platform, player, playerData)

            IncapacitatedCompat.resetDamageTracking(player)
        }catch (exception: ReflectiveOperationException) {
            if (!IncapacitatedCompat.reflectionWarningLogged) {
                IncapacitatedCompat.reflectionWarningLogged = true
                IncapacitatedCompat.LOGGER.warn("Could not prepare Mortal Resolve's final death for Incapacitated.", exception)
            }
        }catch (exception: RuntimeException) {
            if (!IncapacitatedCompat.reflectionWarningLogged) {
                IncapacitatedCompat.reflectionWarningLogged = true
                IncapacitatedCompat.LOGGER.warn("Could not prepare Mortal Resolve's final death for Incapacitated.", exception)
            }
        }
    }

     /**
 * Ends Mortal Resolve after one minute. Earlier/final death handling is owned by MortalResolveManager.
 */
    fun forceMortalResolveDeath(player: ServerPlayer) {
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
