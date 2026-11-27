package andromeda.origins.mixin

import andromeda.origins.compat.UndetectableCompat
import andromeda.origins.compat.VeilbornStealthCompat
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.ItemInHandRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo


@Mixin(ItemInHandRenderer::class)
abstract class UndetectableHeldItemRendererMixin {
    @Inject(method = ["renderArmWithItem"], at = [At("HEAD")], cancellable = true) private fun `andromeda$hideFirstPersonHeldItem`(
    player: AbstractClientPlayer,
    tickDelta: Float,
    pitch: Float,
    hand: InteractionHand,
    swingProgress: Float,
    item: ItemStack,
    equipProgress: Float,
    matrices: PoseStack,
    vertexConsumers: MultiBufferSource,
    light: Int,
    ci: CallbackInfo
    ) {
        if (UndetectableCompat.isUndetectable(player) || VeilbornStealthCompat.isVeilStep(player)) {
            ci.cancel()
        }
    }
}
