package andromeda.origins.mixin

import andromeda.origins.compat.VeilbornStealthCompat


 /** Local-only purple vignette while the player is inside Veil Step.  */
@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.gui.Gui::class)
abstract class VeilStepHudMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = ["render"], at = [org.spongepowered.asm.mixin.injection.At("TAIL")]) private fun `andromeda$renderVeilStepState`(graphics: net.minecraft.client.gui.GuiGraphics, tickCounter: net.minecraft.client.DeltaTracker, ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfo?) {
        val client: net.minecraft.client.Minecraft = net.minecraft.client.Minecraft.getInstance()
        if (client.player == null || !VeilbornStealthCompat.isVeilStep(client.player)) return

        val width: kotlin.Int = graphics.guiWidth()
        val height: kotlin.Int = graphics.guiHeight()
        val bands: kotlin.Int = 14
        val step: kotlin.Int = 4

                // Soft violet edge falloff: strongest at the outside of the screen and fading completely
        // toward the center. No text, frame, or opaque center treatment. Corners naturally deepen
        // because the horizontal and vertical falloffs overlap, giving it a true vignette feel.
        for (i in 0 ..< bands) {
            val strength: kotlin.Float = (bands - i) / bands.toFloat()
            val alpha: kotlin.Int = kotlin.math.max(2, kotlin.math.min(72, (72.0f * strength * strength).toInt()))
            val color: kotlin.Int = (alpha shl 24) or 0x5A1478
            val inset: kotlin.Int = i * step
            val next: kotlin.Int = inset + step
            if (width <= next * 2 || height <= next * 2) break

            graphics.fill(inset, inset, width - inset, next, color)
            graphics.fill(inset, height - next, width - inset, height - inset, color)
            graphics.fill(inset, next, next, height - next, color)
            graphics.fill(width - next, next, width - inset, height - next, color)
        }
    }
}
