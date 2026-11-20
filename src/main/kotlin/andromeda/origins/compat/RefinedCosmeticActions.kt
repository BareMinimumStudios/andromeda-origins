package andromeda.origins.compat

import dev.overgrown.apoli.action.ActionType
import dev.overgrown.apoli.action.ActionTypes
import dev.overgrown.apoli.action.builtin.entity.SpawnParticlesAction
import dev.overgrown.apoli.action.builtin.entity.PlaySoundAction
import dev.overgrown.apoli.condition.context.EntityCtx
import dev.overgrown.apoli.data.Expression
import net.minecraft.resources.ResourceLocation

/** Native Opoli codecs/conditions/positioning retained; only presentation density and volume change. */
object RefinedCosmeticActions {
    fun register() {
        val particles = SpawnParticlesAction()
        ActionTypes.ENTITY.register(ResourceLocation.parse("andromeda_origins:refined_particles"), object : ActionType<EntityCtx, SpawnParticlesAction.Cfg> {
            override fun codec() = particles.codec()
            override fun run(c: SpawnParticlesAction.Cfg, ctx: EntityCtx) {
                val n = OriginFxStyle.particles(ctx.raw(), c.count().evalInt(ctx.raw())) ?: return
                particles.run(SpawnParticlesAction.Cfg(c.particle(), c.bientityCondition(), Expression.constant(n.toDouble()), c.speed(), false,
                    c.spread(), c.offsetX(), c.offsetY(), c.offsetZ(), c.velocityX(), c.velocityY(), c.velocityZ(), c.space(), c.modelPart()), ctx)
            }
        })
        val sound = PlaySoundAction()
        ActionTypes.ENTITY.register(ResourceLocation.parse("andromeda_origins:refined_sound"), object : ActionType<EntityCtx, PlaySoundAction.Cfg> {
            override fun codec() = sound.codec()
            override fun run(c: PlaySoundAction.Cfg, ctx: EntityCtx) {
                if (!OriginFxStyle.sound(ctx.raw(), c.sound().toString() + ":" + c.pitch())) return
                sound.run(PlaySoundAction.Cfg(c.sound(), c.category(), OriginFxStyle.volume(c.volume()), c.pitch(), c.followEntity(), c.global(), c.internal(), c.index()), ctx)
            }
        })
    }
}
