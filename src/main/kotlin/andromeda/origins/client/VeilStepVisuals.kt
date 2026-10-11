package andromeda.origins.client

import andromeda.origins.platform.ClientPlatform
import andromeda.origins.compat.VeilbornStealthCompat


 /**
 * Client-local Veil perception.
 *
 *
 * While the local player is inside Veil Step, living entity models are replaced by sparse
 * shadow-cloud particle echoes. Particle creation is entirely local: it does not mutate entities,
 * sync extra state to the server, or change what any other client renders.
 */
object VeilStepVisuals {
    const val ECHO_RADIUS: kotlin.Double = 48.0
    const val MAX_ECHO_ENTITIES: kotlin.Int = 64
    var veilTicks: kotlin.Int = 0

    fun register() {
        ClientPlatform.endTick(java.util.function.Consumer callback@ { client: net.minecraft.client.Minecraft -> if (client.player == null || client.level == null || !VeilbornStealthCompat.isVeilStep(client.player)) {
            VeilStepVisuals.veilTicks = 0
            return@callback
        }

                    // Every other client tick is enough to maintain a continuous smoky silhouette while
            // keeping dense mob areas from turning into an unnecessary particle flood.
        if ((VeilStepVisuals.veilTicks++ and 1) != 0) return@callback
        VeilStepVisuals.emitLivingEchoes(client)

        })
    }

    fun replaceLivingRender(entity: net.minecraft.world.entity.LivingEntity?): kotlin.Boolean {
        val client: net.minecraft.client.Minecraft = net.minecraft.client.Minecraft.getInstance()
        return entity != null && client.player != null && VeilbornStealthCompat.isVeilStep(client.player)
    }

    private fun emitLivingEchoes(client: net.minecraft.client.Minecraft) {
        val level = client.level ?: return
        val viewer = client.player ?: return
        val bounds: net.minecraft.world.phys.AABB = viewer.getBoundingBox().inflate(VeilStepVisuals.ECHO_RADIUS)
        val living: kotlin.collections.MutableList<net.minecraft.world.entity.LivingEntity> = level.getEntitiesOfClass<net.minecraft.world.entity.LivingEntity>(net.minecraft.world.entity.LivingEntity::class.java, bounds,
        java.util.function.Predicate { entity: net.minecraft.world.entity.LivingEntity -> entity.isAlive()})
        living.sortWith(java.util.Comparator.comparingDouble<net.minecraft.world.entity.LivingEntity>(java.util.function.ToDoubleFunction { entity: net.minecraft.world.entity.LivingEntity -> viewer.distanceToSqr(entity)}))

        val limit: kotlin.Int = kotlin.math.min(VeilStepVisuals.MAX_ECHO_ENTITIES, living.size)
        for (i in 0 ..< limit) {
            val entity: net.minecraft.world.entity.LivingEntity = living.get(i)
            val distanceSqr: kotlin.Double = viewer.distanceToSqr(entity)
            val particles: kotlin.Int = if (distanceSqr <= 16.0 * 16.0)
            7
            else
            if (distanceSqr <= 32.0 * 32.0) 5 else 3
            VeilStepVisuals.emitEcho(level, entity, particles, VeilStepVisuals.veilTicks)
        }
    }

    private fun emitEcho(level: net.minecraft.client.multiplayer.ClientLevel, entity: net.minecraft.world.entity.LivingEntity, particles: kotlin.Int, tick: kotlin.Int) {
        val width: kotlin.Double = kotlin.math.max(0.22, entity.getBbWidth() * 0.56)
        val height: kotlin.Double = kotlin.math.max(0.55, entity.getBbHeight().toDouble())
        val phaseBase: kotlin.Double = entity.getId() * 0.713 + tick * 0.31

        for (i in 0 ..< particles) {
            val phase: kotlin.Double = phaseBase + i * 2.3999632297
            val layer: kotlin.Double = (i + 0.5) / particles
            val radius: kotlin.Double = width * (0.32 + 0.50 * kotlin.math.sin(layer * java.lang.Math.PI))
            val x: kotlin.Double = entity.getX() + kotlin.math.cos(phase) * radius
            val y: kotlin.Double = entity.getY() + 0.08 + layer * height * 0.88
            val z: kotlin.Double = entity.getZ() + kotlin.math.sin(phase) * radius

            level.addAlwaysVisibleParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, x, y, z,
            kotlin.math.cos(phase) * 0.004, 0.008, kotlin.math.sin(phase) * 0.004)
            if ((i and 1) == 0) {
                level.addAlwaysVisibleParticle(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, x, y, z,
                0.0, 0.004, 0.0)
            }
        }
    }
}
