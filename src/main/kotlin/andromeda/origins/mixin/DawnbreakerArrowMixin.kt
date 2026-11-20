package andromeda.origins.mixin

@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.entity.projectile.AbstractArrow::class)
abstract class DawnbreakerArrowMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = ["tick"], at = [org.spongepowered.asm.mixin.injection.At("HEAD")], cancellable = true)
    private fun expireLightArrow(ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfo) {
        val arrow = this as Any as net.minecraft.world.entity.projectile.AbstractArrow
        if (andromeda.origins.compat.SovereignArts.isFrozen(arrow)) { ci.cancel(); return }
        andromeda.origins.compat.Dawnbreaker.lightArrowTrail(arrow)
        if (!arrow.level().isClientSide && "andromeda_dawn_light" in arrow.tags && arrow.tickCount >= 24) {
            arrow.discard(); ci.cancel()
        }
    }
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = ["onHitEntity"], at = [org.spongepowered.asm.mixin.injection.At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z")])
    private fun confirmedImpact(target: net.minecraft.world.entity.Entity, source: net.minecraft.world.damagesource.DamageSource, amount: Float, original: com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean>): Boolean {
        val damaged = original.call(target, source, amount)
        val arrow = this as Any as net.minecraft.world.entity.projectile.AbstractArrow
        val caster = arrow.owner as? net.minecraft.server.level.ServerPlayer
        if (damaged && caster != null && "andromeda_dawn_light" in arrow.tags) andromeda.origins.compat.DawnbreakerFx.cue(caster, 3, target.position())
        return damaged
    }
    @org.spongepowered.asm.mixin.injection.Inject(method = ["onHitEntity"], at = [org.spongepowered.asm.mixin.injection.At("HEAD")])
    private fun lightDamage(hit: net.minecraft.world.phys.EntityHitResult, ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfo) {
        andromeda.origins.compat.Dawnbreaker.prepareLightArrowDamage(this as Any as net.minecraft.world.entity.projectile.AbstractArrow)
    }
    @org.spongepowered.asm.mixin.injection.Inject(method = ["onHitEntity"], at = [org.spongepowered.asm.mixin.injection.At("TAIL")])
    private fun lightImpact(hit: net.minecraft.world.phys.EntityHitResult, ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfo) {
        andromeda.origins.compat.Dawnbreaker.lightArrowImpact(this as Any as net.minecraft.world.entity.projectile.AbstractArrow, hit.location)
    }
    @org.spongepowered.asm.mixin.injection.Inject(method = ["onHitBlock"], at = [org.spongepowered.asm.mixin.injection.At("TAIL")])
    private fun lightBlockImpact(hit: net.minecraft.world.phys.BlockHitResult, ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfo) {
        andromeda.origins.compat.Dawnbreaker.lightArrowImpact(this as Any as net.minecraft.world.entity.projectile.AbstractArrow, hit.location)
    }

}
