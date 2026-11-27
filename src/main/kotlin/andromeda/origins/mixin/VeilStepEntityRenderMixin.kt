package andromeda.origins.mixin

import andromeda.origins.client.VeilStepVisuals
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRenderDispatcher
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo


 /** Hides Veil Step casters for all viewers and replaces living models for viewers inside the Veil. */
@Mixin(EntityRenderDispatcher::class)
abstract class VeilStepEntityRenderMixin {
    @Inject(method = ["render"], at = [At("HEAD")], cancellable = true) private fun <E : Entity>`andromeda$renderLivingAsVeilEcho`(
    entity: E,
    x: Double,
    y: Double,
    z: Double,
    rotationYaw: Float,
    partialTicks: Float,
    poseStack: PoseStack,
    buffer: MultiBufferSource,
    packedLight: Int,
    ci: CallbackInfo
    ) {
        val living = entity as? LivingEntity
        if (living != null && VeilStepVisuals.replaceLivingRender(entity)) {
            ci.cancel()
        }
    }
}
