package andromeda.origins.mixin

/** Scoped only to our synchronous call into the installed Dawnquiver HUD. Does not mutate item-use state. */
@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.entity.LivingEntity::class)
abstract class DawnquiverHudTimerMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = ["getUseItemRemainingTicks"], at = [org.spongepowered.asm.mixin.injection.At("HEAD")], cancellable = true)
    private fun renderDawnTimer(ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Int>) {
        andromeda.origins.client.DawnquiverClientCompat.remainingForHud()?.let { ci.returnValue = it }
    }
}
