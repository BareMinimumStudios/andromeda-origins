package andromeda.origins.mixin

@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.gui.Gui::class)
abstract class DawnbreakerHudMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = ["render"], at = [org.spongepowered.asm.mixin.injection.At("TAIL")])
    private fun renderChorus(graphics: net.minecraft.client.gui.GuiGraphics, delta: net.minecraft.client.DeltaTracker,
        ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfo) {
        andromeda.origins.client.DawnquiverClientCompat.renderHud(graphics, delta)
        andromeda.origins.client.SovereignVisuals.renderHud(graphics)
    }
}
