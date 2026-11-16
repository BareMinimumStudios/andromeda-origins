package andromeda.origins.mixin

@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.entity.projectile.AbstractArrow::class)
abstract class DawnbreakerArrowMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = ["tick"], at = [org.spongepowered.asm.mixin.injection.At("HEAD")], cancellable = true)
    private fun expireLightArrow(ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfo) {
        val arrow = this as Any as net.minecraft.world.entity.projectile.AbstractArrow
        andromeda.origins.compat.Dawnbreaker.lightArrowTrail(arrow)
        if (!arrow.level().isClientSide && "andromeda_dawn_light" in arrow.tags && arrow.tickCount >= 24) {
            arrow.discard(); ci.cancel()
        }
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
