package andromeda.origins.mixin

import andromeda.origins.compat.VeilbornStealthCompat
import andromeda.origins.compat.UndetectableCompat


@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.renderer.ItemInHandRenderer::class)
abstract class UndetectableHeldItemRendererMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = ["renderArmWithItem"], at = [org.spongepowered.asm.mixin.injection.At("HEAD")], cancellable = true) private fun `andromeda$hideFirstPersonHeldItem`(
    player: net.minecraft.client.player.AbstractClientPlayer,
    tickDelta: kotlin.Float,
    pitch: kotlin.Float,
    hand: net.minecraft.world.InteractionHand,
    swingProgress: kotlin.Float,
    item: net.minecraft.world.item.ItemStack,
    equipProgress: kotlin.Float,
    matrices: com.mojang.blaze3d.vertex.PoseStack,
    vertexConsumers: net.minecraft.client.renderer.MultiBufferSource,
    light: kotlin.Int,
    ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfo
    ) {
        if (UndetectableCompat.isUndetectable(player) || VeilbornStealthCompat.isVeilStep(player)) {
            ci.cancel()
        }
    }
}
