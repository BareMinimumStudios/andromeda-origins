package andromeda.origins.mixin

import andromeda.origins.client.DawnbreakerFxClient
import andromeda.origins.client.DawnquiverClientCompat
import andromeda.origins.client.SovereignVisuals
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiGraphics
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(Gui::class)
abstract class DawnbreakerHudMixin {
    @Inject(method = ["render"], at = [At("TAIL")])
    private fun renderChorus(graphics: GuiGraphics, delta: DeltaTracker,
        ci: CallbackInfo) {
        DawnquiverClientCompat.renderHud(graphics, delta)
        SovereignVisuals.renderHud(graphics)
        DawnbreakerFxClient.renderImpact(graphics)
    }
}
