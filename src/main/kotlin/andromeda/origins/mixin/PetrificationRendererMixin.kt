package andromeda.origins.mixin

import andromeda.origins.client.PetrificationVisuals
import net.minecraft.client.DeltaTracker
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.server.packs.resources.ResourceProvider
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(GameRenderer::class)
abstract class PetrificationRendererMixin {
    @Inject(method = ["render"], at = [At("HEAD")])
    private fun `andromeda$preparePetrification`(delta: DeltaTracker, renderLevel: Boolean, ci: CallbackInfo) {
        PetrificationVisuals.prepare()
    }

    @Inject(method = ["render"], at = [At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V", shift = At.Shift.AFTER)])
    private fun `andromeda$renderPetrification`(delta: DeltaTracker, renderLevel: Boolean, ci: CallbackInfo) {
        PetrificationVisuals.render(delta.gameTimeDeltaTicks)
    }

    @Inject(method = ["reloadShaders"], at = [At("HEAD")])
    private fun `andromeda$reloadPetrification`(provider: ResourceProvider, ci: CallbackInfo) {
        PetrificationVisuals.reset()
    }

    @Inject(method = ["close"], at = [At("HEAD")])
    private fun `andromeda$closePetrification`(ci: CallbackInfo) {
        PetrificationVisuals.reset()
    }
}
