package andromeda.origins.client

import andromeda.origins.platform.ClientPlatform
import andromeda.origins.platform.Platform
import dev.overgrown.apoli.power.PowerContainer
import dev.overgrown.apoli.power.PowerResources
import java.util.UUID
import java.util.function.Supplier
import kotlin.math.roundToInt
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import org.slf4j.LoggerFactory

/** Optional library integration: execute Simply Swords' own visuals/HUD; never bundle its implementation or assets. */
object DawnquiverClientCompat {
    private fun id(path: String) = ResourceLocation.parse("andromeda_origins:dawnbreaker/" + path)
    private val bow = id("bow")
    private val light = id("bow_radiance")
    private val radiance = id("radiance")
    private class VisualApi {
        val type = Class.forName("net.sweenus.simplyswords.entity.DawnquiverBowVisualEntity")
        val constructor = type.constructors.first { it.parameterCount == 12 }
        val progress = type.getMethod("setDrawProgress", Float::class.javaPrimitiveType)
        val release = type.getMethod("markReleased")
        val lifetime = type.getMethod("setLifetime", Int::class.javaPrimitiveType)
    }
    private class HudApi {
        val hud = Gui::class.java.declaredMethods.first { it.name.contains("renderDawnquiverHud") && it.parameterCount == 6 }.apply { isAccessible = true }
        val draw = Gui::class.java.declaredMethods.first { it.name.contains("renderDawnquiverDrawBar") && it.parameterCount == 5 }.apply { isAccessible = true }
        val push = Class.forName("net.sweenus.simplyswords.client.api.SimplySwordsClientAPI").getMethod("pushWeaponHudTransform", GuiGraphics::class.java)
        val registry = Class.forName("net.sweenus.simplyswords.registry.ComponentTypeRegistry")
        @Suppress("UNCHECKED_CAST")
        val charge = (registry.getField("STORED_CHARGE").get(null) as Supplier<*>).get() as DataComponentType<Any>
        val chargeValue = Class.forName("net.sweenus.simplyswords.item.component.StoredChargeComponent").getConstructor(Int::class.javaPrimitiveType)
        @Suppress("UNCHECKED_CAST")
        val capacity = try { (registry.getField("DAWN_CHORUS_CAPACITY").get(null) as Supplier<*>).get() as DataComponentType<Int> } catch (_: NoSuchFieldException) { null }
        val effectConfig = Class.forName("net.sweenus.simplyswords.config.Config").getField("uniqueEffects").get(null).let { it.javaClass.getField("dawnquiver").get(it) }
        val duration = effectConfig.javaClass.getField("drawDuration")
    }
    private data class Ghost(val entity: Entity, var released: Boolean = false)
    private val ghosts = mutableMapOf<UUID, Ghost>()
    private var world: ClientLevel? = null
    private var nextId = -2_000_000_000
    private var visualApi: VisualApi? = null
    private var hudApi: HudApi? = null
    private var attempted = false
    private var hudAttempted = false
    private val hudRemaining = ThreadLocal<Int?>()
    private val logger = LoggerFactory.getLogger("Andromeda Origins/Dawnquiver")
    fun remainingForHud(): Int? = hudRemaining.get()
    fun register() { ClientPlatform.endTick { client -> update(client) } }
    private fun value(entity: LivingEntity, name: String) = PowerContainer.of(entity)?.let { PowerResources.read(it, id("bow_" + name)).orElse(0.0) } ?: 0.0
    private fun update(client: Minecraft) {
        val level = client.level
        if (world !== level) { ghosts.values.forEach { it.entity.discard() }; ghosts.clear(); world = level }
        if (level == null || client.player == null || !Platform.isModLoaded("simplyswords")) return
        if (!attempted) {
            attempted = true
            try { visualApi = VisualApi() } catch (error: ReflectiveOperationException) { logger.warn("Dawnquiver bow renderer API unavailable", error) }
              catch (error: LinkageError) { logger.warn("Dawnquiver bow renderer could not link", error) }
              catch (error: NoSuchElementException) { logger.warn("This Simply Swords build has no supported Dawnquiver visual constructor", error) }
        }
        val api = visualApi ?: return
        val wanted = mutableSetOf<UUID>()
        for (owner in level.players()) {
            val c = PowerContainer.of(owner) ?: continue
            if (!owner.isAlive || owner.isSpectator || !c.hasPower(bow) || c.isSuppressed(bow) ||
                !c.hasPower(radiance) || c.isSuppressed(radiance) || PowerResources.read(c, light).orElse(0.0) <= 0 ||
                owner.isInvisibleTo(client.player) || VeilStepVisuals.replaceLivingRender(owner) || client.player!!.distanceToSqr(owner) > 48.0 * 48.0) continue
            val draw = value(owner, "draw")
            val fade = value(owner, "afterglow")
            if (draw <= 0 && fade <= 0) continue
            wanted.add(owner.uuid)
            try {
                var ghost = ghosts[owner.uuid]
                if (ghost == null || (draw > 0 && ghost.released)) {
                    ghost?.entity?.discard()
                    val entity = api.constructor.newInstance(level, owner, InteractionHand.MAIN_HAND,
                        owner.x, owner.y, owner.z, owner.yRot, owner.xRot, 100000, 1f, owner.id and 4095, 0) as Entity
                    entity.id = nextId--
                    entity.xo = owner.xo; entity.yo = owner.yo; entity.zo = owner.zo
                    entity.yRotO = owner.yRotO; entity.xRotO = owner.xRotO
                    level.addEntity(entity)
                    ghost = Ghost(entity); ghosts[owner.uuid] = ghost
                }
                val entity = ghost.entity
                entity.setPos(owner.x, owner.y, owner.z)
                entity.xo = owner.xo; entity.yo = owner.yo; entity.zo = owner.zo
                entity.yRot = owner.yRot; entity.xRot = owner.xRot
                api.progress.invoke(entity, (value(owner, "display_draw") / 60).toFloat().coerceIn(0f, 1f))
                if (draw <= 0 && !ghost.released) { api.release.invoke(entity); ghost.released = true }
                api.lifetime.invoke(entity, if (ghost.released) entity.tickCount + fade.toInt().coerceAtLeast(1) else 100000)
            } catch (error: ReflectiveOperationException) {
                logger.warn("Dawnquiver visual update failed; removing the client-only bow", error)
                ghosts.values.forEach { it.entity.discard() }; ghosts.clear(); visualApi = null
                break
            }
        }
        for (key in ghosts.keys.toList()) if (key !in wanted) ghosts.remove(key)?.entity?.discard()
    }
    fun renderHud(g: GuiGraphics, delta: DeltaTracker) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val c = PowerContainer.of(player) ?: return
        if (client.options.hideGui || player.isSpectator || !c.hasPower(bow) || c.isSuppressed(bow) || !Platform.isModLoaded("simplyswords")) return
        if (value(player, "draw") <= 0 && value(player, "afterglow") <= 0) return
        if (!hudAttempted) {
            hudAttempted = true
            try { hudApi = HudApi() } catch (error: ReflectiveOperationException) { logger.warn("Dawnquiver HUD API unavailable", error) }
              catch (error: NoSuchElementException) { logger.warn("Dawnquiver HUD helpers were not found", error) }
              catch (error: LinkageError) { logger.warn("Dawnquiver HUD API could not link", error) }
        }
        val api = hudApi ?: return
        val item = BuiltInRegistries.ITEM.get(ResourceLocation.parse("simplyswords:dawnquiver"))
        // Let the real weapon's own HUD retain priority when it is actually equipped.
        if (player.mainHandItem.`is`(item) || player.offhandItem.`is`(item)) return
        val stack = item.defaultInstance
        val chorus = value(player, "chorus").toInt().coerceIn(0, 3)
        stack.set(api.charge, api.chargeValue.newInstance(chorus)); api.capacity?.let { stack.set(it, 3) }
        val pose = g.pose().last()
        try {
            val fraction = (value(player, "draw") / 60.0).coerceIn(0.0, 1.0)
            val duration = (api.duration.get(api.effectConfig) as Number).toDouble().coerceAtLeast(4.0)
            hudRemaining.set(stack.getUseDuration(player) - (fraction * duration).roundToInt())
            api.hud.invoke(null, g, client, delta, stack, chorus, 3)
            if (value(player, "draw") > 0) {
                api.push.invoke(null, g)
                api.draw.invoke(null, g, client, delta, stack, chorus)
                g.pose().popPose()
            }
        } catch (error: ReflectiveOperationException) {
            logger.warn("Dawnquiver HUD render failed; disabling this optional HUD bridge", error); hudApi = null
        } finally {
            hudRemaining.remove()
            while (g.pose().last() !== pose) g.pose().popPose()
        }
    }
}
