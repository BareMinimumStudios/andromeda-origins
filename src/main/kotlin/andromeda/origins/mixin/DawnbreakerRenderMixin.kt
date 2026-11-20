package andromeda.origins.mixin

@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.renderer.entity.EntityRenderDispatcher::class)
abstract class DawnbreakerRenderMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = ["render"], at = [org.spongepowered.asm.mixin.injection.At("TAIL")])
    private fun <E : net.minecraft.world.entity.Entity> dawnbreakerRender(
        entity: E, x: Double, y: Double, z: Double, yaw: Float, delta: Float,
        pose: com.mojang.blaze3d.vertex.PoseStack, buffers: net.minecraft.client.renderer.MultiBufferSource, light: Int,
        ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfo
    ) {
        if (andromeda.origins.client.DawnbreakerFxClient.renderingGhost) return
        andromeda.origins.client.DawnbreakerVisuals.renderProjectile(entity, x, y, z, pose, buffers)
        val living = entity as? net.minecraft.world.entity.LivingEntity ?: return
        andromeda.origins.client.DawnbreakerVisuals.render(living, x, y, z, delta, pose, buffers)
    }
}
