package andromeda.origins.mixin

import andromeda.origins.compat.VeilbornStealthCompat
import kotlin.math.max
import kotlin.math.min
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiGraphics
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo


 /** Local-only purple vignette while the player is inside Veil Step.  */
@Mixin(Gui::class)
abstract class VeilStepHudMixin {
    @Inject(method = ["render"], at = [At("TAIL")]) private fun `andromeda$renderVeilStepState`(graphics: GuiGraphics, tickCounter: DeltaTracker, ci: CallbackInfo?) {
        val client: Minecraft = Minecraft.getInstance()
        if (client.player == null || !VeilbornStealthCompat.isVeilStep(client.player)) return

        val width: Int = graphics.guiWidth()
        val height: Int = graphics.guiHeight()
        val bands: Int = 14
        val step: Int = 4

                // Soft violet edge falloff: strongest at the outside of the screen and fading completely
        // toward the center. No text, frame, or opaque center treatment. Corners naturally deepen
        // because the horizontal and vertical falloffs overlap, giving it a true vignette feel.
        for (i in 0 ..< bands) {
            val strength: Float = (bands - i) / bands.toFloat()
            val alpha: Int = max(2, min(72, (72.0f * strength * strength).toInt()))
            val color: Int = (alpha shl 24) or 0x5A1478
            val inset: Int = i * step
            val next: Int = inset + step
            if (width <= next * 2 || height <= next * 2) break

            graphics.fill(inset, inset, width - inset, next, color)
            graphics.fill(inset, height - next, width - inset, height - inset, color)
            graphics.fill(inset, next, next, height - next, color)
            graphics.fill(width - next, next, width - inset, height - next, color)
        }
    }
}
