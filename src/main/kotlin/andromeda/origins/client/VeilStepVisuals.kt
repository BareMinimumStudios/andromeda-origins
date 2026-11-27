package andromeda.origins.client

import andromeda.origins.compat.VeilbornStealthCompat
import andromeda.origins.platform.ClientPlatform
import java.util.Comparator
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.function.ToDoubleFunction
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.AABB


 /**
 * Client-local Veil perception.
 *
 *
 * While the local player is inside Veil Step, living entity models are replaced by sparse
 * shadow-cloud particle echoes. Particle creation is entirely local: it does not mutate entities,
 * sync extra state to the server, or change what any other client renders.
 */
object VeilStepVisuals {
    const val ECHO_RADIUS: Double = 48.0
    const val MAX_ECHO_ENTITIES: Int = 64
    var veilTicks: Int = 0

    fun register() {
        ClientPlatform.endTick(Consumer callback@ { client: Minecraft -> if (client.player == null || client.level == null || !VeilbornStealthCompat.isVeilStep(client.player)) {
            VeilStepVisuals.veilTicks = 0
            return@callback
        }

                    // Every other client tick is enough to maintain a continuous smoky silhouette while
            // keeping dense mob areas from turning into an unnecessary particle flood.
        if ((VeilStepVisuals.veilTicks++ and 3) != 0) return@callback
        VeilStepVisuals.emitLivingEchoes(client)

        })
    }

    fun replaceLivingRender(entity: LivingEntity?): Boolean {
        val client: Minecraft = Minecraft.getInstance()
        return VeilbornStealthCompat.renderAsVeilCloud(entity, client.player)
    }

    private fun emitLivingEchoes(client: Minecraft) {
        val level = client.level ?: return
        val viewer = client.player ?: return
        val bounds: AABB = viewer.getBoundingBox().inflate(VeilStepVisuals.ECHO_RADIUS)
        val living: MutableList<LivingEntity> = level.getEntitiesOfClass<LivingEntity>(LivingEntity::class.java, bounds,
        Predicate { entity: LivingEntity -> entity.isAlive()})
        living.sortWith(Comparator.comparingDouble<LivingEntity>(ToDoubleFunction { entity: LivingEntity -> viewer.distanceToSqr(entity)}))

        val limit: Int = min(VeilStepVisuals.MAX_ECHO_ENTITIES, living.size)
        for (i in 0 ..< limit) {
            val entity: LivingEntity = living.get(i)
            val distanceSqr: Double = viewer.distanceToSqr(entity)
            val particles: Int = if (distanceSqr <= 16.0 * 16.0)
            4
            else
            if (distanceSqr <= 32.0 * 32.0) 3 else 2
            VeilStepVisuals.emitEcho(level, entity, particles, VeilStepVisuals.veilTicks)
        }
    }

    private fun emitEcho(level: ClientLevel, entity: LivingEntity, particles: Int, tick: Int) {
        val width: Double = max(0.22, entity.getBbWidth() * 0.56)
        val height: Double = max(0.55, entity.getBbHeight().toDouble())
        val phaseBase: Double = entity.getId() * 0.713 + tick * 0.31

        for (i in 0 ..< particles) {
            val phase: Double = phaseBase + i * 2.3999632297
            val layer: Double = (i + 0.5) / particles
            val radius: Double = width * (0.32 + 0.50 * sin(layer * Math.PI))
            val x: Double = entity.getX() + cos(phase) * radius
            val y: Double = entity.getY() + 0.08 + layer * height * 0.88
            val z: Double = entity.getZ() + sin(phase) * radius

            level.addAlwaysVisibleParticle(ParticleTypes.SMOKE, x, y, z,
            cos(phase) * 0.004, 0.008, sin(phase) * 0.004)
            if ((i and 1) == 0) {
                level.addAlwaysVisibleParticle(ParticleTypes.REVERSE_PORTAL, x, y, z,
                0.0, 0.004, 0.0)
            }
        }
    }
}
