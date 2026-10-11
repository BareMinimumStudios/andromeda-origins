package andromeda.origins.mixin




 /** Replaces Overgrown's stock Origin window with Andromeda's standalone wide reader.  */
@org.spongepowered.asm.mixin.Mixin(dev.overgrown.origins.client.screen.OriginDisplayScreen::class)
abstract class OriginDisplayScreenRichReaderMixin {
    @org.spongepowered.asm.mixin.Shadow
    protected var guiLeft: kotlin.Int = 0
    @org.spongepowered.asm.mixin.Shadow
    protected var guiTop: kotlin.Int = 0


    @org.spongepowered.asm.mixin.injection.Inject(method = ["renderOriginWindow"], at = [org.spongepowered.asm.mixin.injection.At("HEAD")], cancellable = true) private fun `andromeda$hideStockOriginWindow`(graphics: net.minecraft.client.gui.GuiGraphics, mouseX: kotlin.Int, mouseY: kotlin.Int, ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfo) {
        if (this as kotlin.Any is dev.overgrown.origins.client.screen.ChooseOriginScreen || this as kotlin.Any is dev.overgrown.origins.client.screen.ViewOriginScreen) {
            ci.cancel()
        }
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = ["renderScrollbar"], at = [org.spongepowered.asm.mixin.injection.At("HEAD")], cancellable = true) private fun `andromeda$hideStockScrollbar`(graphics: net.minecraft.client.gui.GuiGraphics, mouseX: kotlin.Int, mouseY: kotlin.Int, ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfo) {
        if (this as kotlin.Any is dev.overgrown.origins.client.screen.ChooseOriginScreen || this as kotlin.Any is dev.overgrown.origins.client.screen.ViewOriginScreen) {
            ci.cancel()
        }
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = ["mouseClicked"], at = [org.spongepowered.asm.mixin.injection.At("HEAD")], cancellable = true) private fun `andromeda$disableHiddenStockScrollbar`(mouseX: kotlin.Double, mouseY: kotlin.Double, button: kotlin.Int,
    cir: org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<kotlin.Boolean>) {
        if ((this as kotlin.Any is dev.overgrown.origins.client.screen.ChooseOriginScreen || this as kotlin.Any is dev.overgrown.origins.client.screen.ViewOriginScreen)
        && mouseX >= this.guiLeft + 155 && mouseX < this.guiLeft + 164 && mouseY >= this.guiTop + 35 && mouseY < this.guiTop + 169) {
            cir.setReturnValue(false)
        }
    }
}
