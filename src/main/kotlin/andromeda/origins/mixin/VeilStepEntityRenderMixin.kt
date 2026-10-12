package andromeda.origins.mixin

import andromeda.origins.client.VeilStepVisuals


 /** Hides Veil Step casters for all viewers and replaces living models for viewers inside the Veil. */
@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.renderer.entity.EntityRenderDispatcher::class)
abstract class VeilStepEntityRenderMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = ["render"], at = [org.spongepowered.asm.mixin.injection.At("HEAD")], cancellable = true) private fun <E : net.minecraft.world.entity.Entity>`andromeda$renderLivingAsVeilEcho`(
    entity: E,
    x: kotlin.Double,
    y: kotlin.Double,
    z: kotlin.Double,
    rotationYaw: kotlin.Float,
    partialTicks: kotlin.Float,
    poseStack: com.mojang.blaze3d.vertex.PoseStack,
    buffer: net.minecraft.client.renderer.MultiBufferSource,
    packedLight: kotlin.Int,
    ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfo
    ) {
        val living = entity as? net.minecraft.world.entity.LivingEntity
        if (living != null && VeilStepVisuals.replaceLivingRender(entity)) {
            ci.cancel()
        }
    }
}
