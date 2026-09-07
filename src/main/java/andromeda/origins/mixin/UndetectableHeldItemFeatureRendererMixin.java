package andromeda.origins.mixin;

import andromeda.origins.compat.UndetectableCompat;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemFeatureRenderer.class)
public abstract class UndetectableHeldItemFeatureRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void andromeda$hideThirdPersonHeldItem(
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        LivingEntity entity,
        float limbAngle,
        float limbDistance,
        float tickDelta,
        float animationProgress,
        float headYaw,
        float headPitch,
        CallbackInfo ci
    ) {
        if (UndetectableCompat.isUndetectable(entity)) {
            ci.cancel();
        }
    }
}
