package andromeda.origins.mixin

import andromeda.origins.client.OriginRichReaderRenderer
import andromeda.origins.client.OriginUiTheme
import andromeda.origins.mixin.ViewOriginScreenAccessor
import dev.overgrown.origins.client.screen.ChooseOriginScreen
import dev.overgrown.origins.client.screen.OriginDisplayScreen
import dev.overgrown.origins.client.screen.SwapOriginScreen
import dev.overgrown.origins.client.screen.ViewOriginScreen
import dev.overgrown.origins.origin.Origin
import dev.overgrown.origins.origin.OriginLayer
import java.util.ArrayList
import kotlin.math.max
import kotlin.math.min
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.network.chat.Component
import net.minecraft.util.Tuple
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo


 /**
 * Flat centered O-key Origin viewer using the same visual language as Andromeda's chooser.
 * Overgrown still owns current-origin state, layer data, swapping and selection networking; this
 * mixin rebuilds only the controls/presentation so it does not depend on translated button labels.
 */
@Mixin(ViewOriginScreen::class)
abstract class ViewOriginScreenRichReaderMixin protected constructor(title: Component, showDirtBackground: Boolean) : OriginDisplayScreen(title, showDirtBackground) {
    @Unique
    var `andromeda$panelLeft`: Int = 0
    @Unique
    var `andromeda$panelTop`: Int = 0
    @Unique
    var `andromeda$panelRight`: Int = 0
    @Unique
    var `andromeda$panelBottom`: Int = 0
    @Unique
    var `andromeda$readerBottom`: Int = 0
    @Unique
    var `andromeda$closeButton`: Button? = null
    @Unique
    var `andromeda$chooseButton`: Button? = null
    @Unique
    var `andromeda$previousButton`: Button? = null
    @Unique
    var `andromeda$nextButton`: Button? = null
    @Unique
    var `andromeda$swapButton`: Button? = null

    @Inject(method = ["init"], at = [At("TAIL")]) private fun `andromeda$layoutViewReader`(ci: CallbackInfo?) {
        val access: ViewOriginScreenAccessor = this as Any as ViewOriginScreenAccessor
        val originLayers: ArrayList<Tuple<OriginLayer, Origin>> = access.`andromeda$getOriginLayers`()

                // Replace the stock controls after Overgrown has initialized its state. This avoids finding
        // buttons by localized text, dimensions or one-letter labels.
        this.clearWidgets()

        val panelWidth: Int = min(660, max(240, this.width - 28))
        this.`andromeda$panelLeft` = (this.width - panelWidth) / 2
        this.`andromeda$panelRight` = this.`andromeda$panelLeft` + panelWidth
        this.`andromeda$panelTop` = max(8, min(14, this.height / 18))
        this.`andromeda$panelBottom` = max(this.`andromeda$panelTop` + 145, this.height - 8)
        this.`andromeda$panelBottom` = min(this.height - 6, this.`andromeda$panelBottom`)

        if (!originLayers.isEmpty()) {
            this.`andromeda$chooseButton` = this.addRenderableWidget<Button>(Button.builder(
            Component.translatable("origins.gui.choose"), Button.OnPress callback@ { b: Button -> val current: Int = access.`andromeda$getCurrentLayerIndex`()
            if (current < 0 || current >= originLayers.size) return@callback
            val list: ArrayList<OriginLayer> = ArrayList<OriginLayer>()
            list.add(originLayers.get(current).getA())
            Minecraft.getInstance().setScreen(ChooseOriginScreen(list, 0, false, false))

            }).bounds(0, 0, 100, 20).build())
            access.`andromeda$setChooseOriginButton`(this.`andromeda$chooseButton`)

            if (originLayers.size > 1) {
                this.`andromeda$previousButton` = this.addRenderableWidget<Button>(Button.builder(Component.literal("<"), Button.OnPress callback@ { b: Button -> val current: Int = (access.`andromeda$getCurrentLayerIndex`() - 1 + originLayers.size) % originLayers.size
                access.`andromeda$setCurrentLayerIndex`(current)
                val value: Tuple<OriginLayer, Origin> = originLayers.get(current)
                this.showOrigin(value.getB(), value.getA(), false)
                access.`andromeda$invokeRefreshChooseButton`()
                access.`andromeda$invokeRefreshSwapButton`()

                }).bounds(0, 0, 20, 20).build())
                this.`andromeda$nextButton` = this.addRenderableWidget<Button>(Button.builder(Component.literal(">"), Button.OnPress callback@ { b: Button -> val current: Int = (access.`andromeda$getCurrentLayerIndex`() + 1) % originLayers.size
                access.`andromeda$setCurrentLayerIndex`(current)
                val value: Tuple<OriginLayer, Origin> = originLayers.get(current)
                this.showOrigin(value.getB(), value.getA(), false)
                access.`andromeda$invokeRefreshChooseButton`()
                access.`andromeda$invokeRefreshSwapButton`()

                }).bounds(0, 0, 20, 20).build())
            }
        } else {
            this.`andromeda$chooseButton` = null
        }

        this.`andromeda$closeButton` = this.addRenderableWidget<Button>(Button.builder(
        Component.translatable("origins.gui.close"), Button.OnPress callback@ { b: Button -> Minecraft.getInstance().setScreen(null)})
        .bounds(0, 0, 100, 20).build())

        this.`andromeda$swapButton` = this.addRenderableWidget<Button>(Button.builder(Component.literal("S"), Button.OnPress callback@ { b: Button -> val current: Int = access.`andromeda$getCurrentLayerIndex`()
        if (current >= 0 && current < originLayers.size) {
            Minecraft.getInstance().setScreen(SwapOriginScreen(originLayers.get(current).getA().id()))
        }

        }).bounds(0, 0, 20, 20).build())
        this.`andromeda$swapButton`!!.setTooltip(Tooltip.create(Component.translatable("origins.gui.swap_origin.button")))
        access.`andromeda$setSwapButton`(this.`andromeda$swapButton`)

        if (this.`andromeda$chooseButton` != null) access.`andromeda$invokeRefreshChooseButton`()
        access.`andromeda$invokeRefreshSwapButton`()

        val choosing: Boolean = this.`andromeda$chooseButton` != null && this.`andromeda$chooseButton`!!.visible
        val footerHeight: Int = if (choosing) 52 else 29
        this.`andromeda$readerBottom` = this.`andromeda$panelBottom` - footerHeight
        `andromeda$positionButtons`(choosing)
    }

    @Unique private fun `andromeda$positionButtons`(choosing: Boolean) {
        val centerX: Int = (this.`andromeda$panelLeft` + this.`andromeda$panelRight`) / 2
        if (choosing) {
            val chooseY: Int = this.`andromeda$readerBottom` + 6
            val footerY: Int = this.`andromeda$readerBottom` + 29
            if (this.`andromeda$chooseButton` != null) this.`andromeda$chooseButton`!!.setPosition(centerX - 50, chooseY)
            if (this.`andromeda$closeButton` != null) this.`andromeda$closeButton`!!.setPosition(centerX - 50, footerY)
            if (this.`andromeda$previousButton` != null) this.`andromeda$previousButton`!!.setPosition(centerX - 79, footerY)
            if (this.`andromeda$nextButton` != null) this.`andromeda$nextButton`!!.setPosition(centerX + 59, footerY)
            if (this.`andromeda$swapButton` != null) this.`andromeda$swapButton`!!.setPosition(centerX + 86, footerY)
        } else {
            val footerY: Int = this.`andromeda$readerBottom` + 6
            if (this.`andromeda$closeButton` != null) this.`andromeda$closeButton`!!.setPosition(centerX - 50, footerY)
            if (this.`andromeda$previousButton` != null) this.`andromeda$previousButton`!!.setPosition(centerX - 79, footerY)
            if (this.`andromeda$nextButton` != null) this.`andromeda$nextButton`!!.setPosition(centerX + 59, footerY)
            if (this.`andromeda$swapButton` != null) this.`andromeda$swapButton`!!.setPosition(centerX + 86, footerY)
        }
    }

    @Inject(method = ["render"], at = [At("TAIL")]) private fun `andromeda$renderViewReader`(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float,
    ci: CallbackInfo?) {
        // Visibility can change after a client-state refresh, so keep footer geometry synchronized.
        val choosing: Boolean = this.`andromeda$chooseButton` != null && this.`andromeda$chooseButton`!!.visible
        val wantedReaderBottom: Int = this.`andromeda$panelBottom` - (if (choosing) 52 else 29)
        if (wantedReaderBottom != this.`andromeda$readerBottom`) {
            this.`andromeda$readerBottom` = wantedReaderBottom
            `andromeda$positionButtons`(choosing)
        }

        `andromeda$renderRichDetails`(graphics, mouseX, mouseY, partialTick)
        OriginUiTheme.flatButton(graphics, this.fontRef(), this.`andromeda$chooseButton`, mouseX, mouseY, false)
        OriginUiTheme.flatButton(graphics, this.fontRef(), this.`andromeda$closeButton`, mouseX, mouseY, false)
        OriginUiTheme.flatButton(graphics, this.fontRef(), this.`andromeda$previousButton`, mouseX, mouseY, false)
        OriginUiTheme.flatButton(graphics, this.fontRef(), this.`andromeda$nextButton`, mouseX, mouseY, false)
        OriginUiTheme.flatButton(graphics, this.fontRef(), this.`andromeda$swapButton`, mouseX, mouseY, false)
    }

    @Unique private fun `andromeda$renderRichDetails`(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val left: Int = this.`andromeda$panelLeft`
        val top: Int = this.`andromeda$panelTop`
        val right: Int = this.`andromeda$panelRight`
        val bottom: Int = this.`andromeda$readerBottom`
        if (right <= left || bottom <= top) return

        OriginUiTheme.panel(graphics, left, top, right, this.`andromeda$panelBottom`)
        graphics.fill(left + 1, bottom, right - 1, bottom + 1, OriginUiTheme.DIVIDER)

        val screenTitle: Component = Component.translatable("andromeda_origins.screen.view_origin.title").withStyle(ChatFormatting.BOLD)
        graphics.drawCenteredString(this.fontRef(), screenTitle, (left + right) / 2, top + 8, OriginUiTheme.TEXT)

        val origin: Origin? = this.getCurrentOrigin()
        val layer: OriginLayer? = this.getCurrentLayer()
        if (origin == null) {
            val empty: Component = Component.translatable("origins.gui.view_origin.empty").withStyle(ChatFormatting.GRAY)
            graphics.drawCenteredString(this.fontRef(), empty, (left + right) / 2,
            top + max(45, (bottom - top) / 2), -0x373738)
            this.setMaxScroll(0)
            return
        }

        if (layer != null) {
            graphics.drawCenteredString(this.fontRef(), layer.name(), (left + right) / 2, top + 21,
            OriginUiTheme.MUTED)
        }

        val readerTop: Int = top + 32
        val maxScroll: Int = OriginRichReaderRenderer.render(
        graphics,
        this.fontRef(),
        origin,
        Minecraft.getInstance().player,
        OriginRichReaderRenderer.Bounds(left, readerTop, right, bottom),
        this.scrollPos,
        mouseX,
        mouseY,
        partialTick
        )
        this.setMaxScroll(maxScroll)
    }
}
