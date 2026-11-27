package andromeda.origins.mixin

import andromeda.origins.compat.Dawnbreaker
import andromeda.origins.compat.DawnbreakerFx
import andromeda.origins.compat.SovereignArts
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.projectile.AbstractArrow
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(AbstractArrow::class)
abstract class DawnbreakerArrowMixin {
    @Inject(method = ["tick"], at = [At("HEAD")], cancellable = true)
    private fun expireLightArrow(ci: CallbackInfo) {
        val arrow = this as Any as AbstractArrow
        if (SovereignArts.isFrozen(arrow)) { ci.cancel(); return }
        Dawnbreaker.lightArrowTrail(arrow)
        if (!arrow.level().isClientSide && "andromeda_dawn_light" in arrow.tags && arrow.tickCount >= 24) {
            arrow.discard(); ci.cancel()
        }
    }
    @WrapOperation(method = ["onHitEntity"], at = [At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z")])
    private fun confirmedImpact(target: Entity, source: DamageSource, amount: Float, original: Operation<Boolean>): Boolean {
        val damaged = original.call(target, source, amount)
        val arrow = this as Any as AbstractArrow
        val caster = arrow.owner as? ServerPlayer
        if (damaged && caster != null && "andromeda_dawn_light" in arrow.tags) DawnbreakerFx.cue(caster, 3, target.position())
        return damaged
    }
    @Inject(method = ["onHitEntity"], at = [At("HEAD")])
    private fun lightDamage(hit: EntityHitResult, ci: CallbackInfo) {
        Dawnbreaker.prepareLightArrowDamage(this as Any as AbstractArrow)
    }
    @Inject(method = ["onHitEntity"], at = [At("TAIL")])
    private fun lightImpact(hit: EntityHitResult, ci: CallbackInfo) {
        Dawnbreaker.lightArrowImpact(this as Any as AbstractArrow, hit.location)
    }
    @Inject(method = ["onHitBlock"], at = [At("TAIL")])
    private fun lightBlockImpact(hit: BlockHitResult, ci: CallbackInfo) {
        Dawnbreaker.lightArrowImpact(this as Any as AbstractArrow, hit.location)
    }

}
