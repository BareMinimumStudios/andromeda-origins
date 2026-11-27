package andromeda.origins.mixin

import andromeda.origins.client.DawnbreakerFxClient
import andromeda.origins.client.DawnbreakerVisuals
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRenderDispatcher
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(EntityRenderDispatcher::class)
abstract class DawnbreakerRenderMixin {
    @Inject(method = ["render"], at = [At("TAIL")])
    private fun <E : Entity> dawnbreakerRender(
        entity: E, x: Double, y: Double, z: Double, yaw: Float, delta: Float,
        pose: PoseStack, buffers: MultiBufferSource, light: Int,
        ci: CallbackInfo
    ) {
        if (DawnbreakerFxClient.renderingGhost) return
        DawnbreakerVisuals.renderProjectile(entity, x, y, z, pose, buffers)
        val living = entity as? LivingEntity ?: return
        DawnbreakerVisuals.render(living, x, y, z, delta, pose, buffers)
    }
}
