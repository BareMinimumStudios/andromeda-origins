package andromeda.origins.mixin

import andromeda.origins.compat.VeilbornStealthCompat


 /** Suppresses vanilla fire rendering during Mirage or Veil Step so the authored Veil visuals stay clean.  */
@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.renderer.entity.EntityRenderDispatcher::class)
abstract class VeilbornFireRenderMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = ["renderFlame"], at = [org.spongepowered.asm.mixin.injection.At("HEAD")], cancellable = true) private fun `andromeda$hideVeilbornFire`(
    matrices: com.mojang.blaze3d.vertex.PoseStack,
    vertexConsumers: net.minecraft.client.renderer.MultiBufferSource,
    entity: net.minecraft.world.entity.Entity,
    rotation: org.joml.Quaternionf,
    ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfo
    ) {
        if (VeilbornStealthCompat.suppressTraces(entity)) {
            ci.cancel()
        }
    }
}
