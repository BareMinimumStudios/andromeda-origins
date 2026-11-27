package andromeda.origins.mixin

import andromeda.origins.compat.VeilbornStealthCompat
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRenderDispatcher
import net.minecraft.world.entity.Entity
import org.joml.Quaternionf
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo


 /** Suppresses vanilla fire rendering during Mirage or Veil Step so the authored Veil visuals stay clean.  */
@Mixin(EntityRenderDispatcher::class)
abstract class VeilbornFireRenderMixin {
    @Inject(method = ["renderFlame"], at = [At("HEAD")], cancellable = true) private fun `andromeda$hideVeilbornFire`(
    matrices: PoseStack,
    vertexConsumers: MultiBufferSource,
    entity: Entity,
    rotation: Quaternionf,
    ci: CallbackInfo
    ) {
        if (VeilbornStealthCompat.suppressTraces(entity)) {
            ci.cancel()
        }
    }
}
