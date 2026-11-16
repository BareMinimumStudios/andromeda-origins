package andromeda.origins.compat

import andromeda.origins.platform.Platform
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3

/** Optional client-built MRPG presets. No library classes are linked by the base mod. */
object MrpgFxBridge {
    private class Access {
        private val fx = Class.forName("com.mrpg_lib.fx.MrpgFx")
        private val params = Class.forName("com.mrpg_lib.fx.FxParams")
        val get = fx.getMethod("get", ResourceLocation::class.java)
        val at = params.getMethod("at", Vec3::class.java)
        val attach = params.getMethod("attach", net.minecraft.world.entity.Entity::class.java)
        val direction = params.getMethod("direction", Vec3::class.java)
        val radius = params.getMethod("radius", Float::class.javaPrimitiveType)
        val intensity = params.getMethod("intensity", Float::class.javaPrimitiveType)
        val color = params.getMethod("color", Int::class.javaPrimitiveType)
        val duration = params.getMethod("duration", Int::class.javaPrimitiveType)
        val packet = Class.forName("com.mrpg_lib.fx.FxPlayPayload").getConstructor(ResourceLocation::class.java, params, Int::class.javaPrimitiveType)
    }
    private var attempted = false
    private var access: Access? = null
    private val warned = mutableSetOf<String>()
    private val logger = org.slf4j.LoggerFactory.getLogger("Andromeda Origins/MRPG FX")

    fun play(level: ServerLevel, position: Vec3, definition: EnhancedFxDefinitions.MrpgDefinition, entity: net.minecraft.world.entity.Entity, atSource: Boolean): Boolean {
        if (!Platform.isModLoaded("mrpg_lib")) return false
        if (!attempted) {
            attempted = true
            try { access = Access() } catch (error: ReflectiveOperationException) {
                logger.warn("MRPG preset FX API unavailable; retaining existing optional particles.")
            } catch (error: LinkageError) {
                logger.warn("MRPG preset FX API could not link; retaining existing optional particles.")
            }
        }
        val api = access ?: return false
        val id = ResourceLocation.tryParse(definition.preset) ?: return false
        if (!definition.radius.isFinite() || !definition.intensity.isFinite()) return false
        try {
            val effect = api.get.invoke(null, id)
            if (effect == null) {
                if (warned.add(id.toString())) logger.warn("Unknown MRPG preset {}; retaining existing particles.", id)
                return false
            }
            val parameters = api.at.invoke(null, position)
            if (definition.attach && atSource) api.attach.invoke(parameters, entity)
            api.direction.invoke(parameters, entity.lookAngle)
            api.radius.invoke(parameters, definition.radius.coerceIn(0.1f, 10f))
            api.intensity.invoke(parameters, definition.intensity.coerceIn(0f, 2f))
            api.color.invoke(parameters, definition.color and 0xffffff)
            // Short, finite bursts need no persistent handles and cannot leave a channel effect running.
            api.duration.invoke(parameters, definition.duration.coerceIn(1, 20))
            val viewers = level.players().filter { it.distanceToSqr(position) <= 64.0 * 64.0 }
            val channel = ResourceLocation.parse("mrpg_lib:fx_play")
            // Use the previous particle path for mixed/older clients; never send an unknown payload.
            if (viewers.isEmpty() || viewers.any { !Platform.canReceiveCosmetic(it, channel) }) return false
            val packet = api.packet.newInstance(id, parameters, 0) as net.minecraft.network.protocol.common.custom.CustomPacketPayload
            for (viewer in viewers) Platform.sendCosmetic(viewer, packet)
            return true
        } catch (error: ReflectiveOperationException) {
            if (warned.add("invoke")) logger.warn("MRPG preset playback failed; retaining existing particles.", error)
            return false
        } catch (error: LinkageError) {
            if (warned.add("link")) logger.warn("MRPG preset playback could not link; retaining existing particles.")
            return false
        }
    }
}
