package andromeda.origins.compat

import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import dev.overgrown.apoli.action.ActionType
import dev.overgrown.apoli.action.ActionTypes
import dev.overgrown.apoli.condition.context.EntityCtx
import net.minecraft.commands.arguments.coordinates.LocalCoordinates
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel

/** Preserves the original local-coordinate particle geometry without Brigadier per point. */
object ParticleSpheres {
    private data class Config(val preset: String)
    private val points: Map<String, List<DoubleArray>> by lazy {
        val stream = requireNotNull(javaClass.getResourceAsStream("/assets/andromeda_origins/particle_spheres.json"))
        stream.bufferedReader().use { reader ->
            JsonParser.parseReader(reader).asJsonObject.entrySet().associate { (name, value) ->
                name to value.asJsonArray.map { point -> point.asJsonArray.map { it.asDouble }.toDoubleArray() }
            }
        }
    }

    fun register() {
        ActionTypes.ENTITY.register(ResourceLocation.fromNamespaceAndPath("andromeda_origins", "particle_sphere"),
            object : ActionType<EntityCtx, Config> {
                override fun codec(): MapCodec<Config> = Codec.STRING.fieldOf("preset").xmap(::Config) { it.preset }
                override fun run(cfg: Config, ctx: EntityCtx) {
                    val entity = ctx.raw()
                    val level = entity.level() as? ServerLevel ?: return
                    val particle = when (cfg.preset) {
                        "soul" -> ParticleTypes.SOUL
                        "water" -> ParticleTypes.FALLING_WATER
                        else -> return
                    }
                    val source = entity.createCommandSourceStack()
                    for (point in points.getValue(cfg.preset)) {
                        val position = LocalCoordinates(point[0], point[1], point[2]).getPosition(source)
                        for (viewer in level.server.playerList.players) {
                            level.sendParticles(viewer, particle, true, position.x, position.y, position.z, 1, 0.0, 0.0, 0.0, 0.0)
                        }
                    }
                }
            })
    }
}
