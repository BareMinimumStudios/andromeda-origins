package andromeda.origins.mixin

import dev.overgrown.origins.client.screen.ChooseOriginScreen
import dev.overgrown.origins.client.screen.OriginDisplayScreen
import dev.overgrown.origins.client.screen.ViewOriginScreen
import net.minecraft.client.gui.GuiGraphics
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable




 /** Replaces Overgrown's stock Origin window with Andromeda's standalone wide reader.  */
@Mixin(OriginDisplayScreen::class)
abstract class OriginDisplayScreenRichReaderMixin {
    @Shadow
    protected var guiLeft: Int = 0
    @Shadow
    protected var guiTop: Int = 0


    @Inject(method = ["renderOriginWindow"], at = [At("HEAD")], cancellable = true) private fun `andromeda$hideStockOriginWindow`(graphics: GuiGraphics, mouseX: Int, mouseY: Int, ci: CallbackInfo) {
        if (this as Any is ChooseOriginScreen || this as Any is ViewOriginScreen) {
            ci.cancel()
        }
    }

    @Inject(method = ["renderScrollbar"], at = [At("HEAD")], cancellable = true) private fun `andromeda$hideStockScrollbar`(graphics: GuiGraphics, mouseX: Int, mouseY: Int, ci: CallbackInfo) {
        if (this as Any is ChooseOriginScreen || this as Any is ViewOriginScreen) {
            ci.cancel()
        }
    }

    @Inject(method = ["mouseClicked"], at = [At("HEAD")], cancellable = true) private fun `andromeda$disableHiddenStockScrollbar`(mouseX: Double, mouseY: Double, button: Int,
    cir: CallbackInfoReturnable<Boolean>) {
        if ((this as Any is ChooseOriginScreen || this as Any is ViewOriginScreen)
        && mouseX >= this.guiLeft + 155 && mouseX < this.guiLeft + 164 && mouseY >= this.guiTop + 35 && mouseY < this.guiTop + 169) {
            cir.setReturnValue(false)
        }
    }
}
