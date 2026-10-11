package andromeda.origins.mixin

import andromeda.origins.client.OriginRichReaderRenderer
import andromeda.origins.client.OriginUiTheme
import andromeda.origins.mixin.ViewOriginScreenAccessor


 /**
 * Flat centered O-key Origin viewer using the same visual language as Andromeda's chooser.
 * Overgrown still owns current-origin state, layer data, swapping and selection networking; this
 * mixin rebuilds only the controls/presentation so it does not depend on translated button labels.
 */
@org.spongepowered.asm.mixin.Mixin(dev.overgrown.origins.client.screen.ViewOriginScreen::class)
abstract class ViewOriginScreenRichReaderMixin protected constructor(title: net.minecraft.network.chat.Component, showDirtBackground: kotlin.Boolean) : dev.overgrown.origins.client.screen.OriginDisplayScreen(title, showDirtBackground) {
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$panelLeft`: kotlin.Int = 0
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$panelTop`: kotlin.Int = 0
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$panelRight`: kotlin.Int = 0
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$panelBottom`: kotlin.Int = 0
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$readerBottom`: kotlin.Int = 0
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$closeButton`: net.minecraft.client.gui.components.Button? = null
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$chooseButton`: net.minecraft.client.gui.components.Button? = null
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$previousButton`: net.minecraft.client.gui.components.Button? = null
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$nextButton`: net.minecraft.client.gui.components.Button? = null
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$swapButton`: net.minecraft.client.gui.components.Button? = null

    @org.spongepowered.asm.mixin.injection.Inject(method = ["init"], at = [org.spongepowered.asm.mixin.injection.At("TAIL")]) private fun `andromeda$layoutViewReader`(ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfo?) {
        val access: ViewOriginScreenAccessor = this as kotlin.Any as ViewOriginScreenAccessor
        val originLayers: java.util.ArrayList<net.minecraft.util.Tuple<dev.overgrown.origins.origin.OriginLayer, dev.overgrown.origins.origin.Origin>> = access.`andromeda$getOriginLayers`()

                // Replace the stock controls after Overgrown has initialized its state. This avoids finding
        // buttons by localized text, dimensions or one-letter labels.
        this.clearWidgets()

        val panelWidth: kotlin.Int = kotlin.math.min(660, kotlin.math.max(240, this.width - 28))
        this.`andromeda$panelLeft` = (this.width - panelWidth) / 2
        this.`andromeda$panelRight` = this.`andromeda$panelLeft` + panelWidth
        this.`andromeda$panelTop` = kotlin.math.max(8, kotlin.math.min(14, this.height / 18))
        this.`andromeda$panelBottom` = kotlin.math.max(this.`andromeda$panelTop` + 145, this.height - 8)
        this.`andromeda$panelBottom` = kotlin.math.min(this.height - 6, this.`andromeda$panelBottom`)

        if (!originLayers.isEmpty()) {
            this.`andromeda$chooseButton` = this.addRenderableWidget<net.minecraft.client.gui.components.Button>(net.minecraft.client.gui.components.Button.builder(
            net.minecraft.network.chat.Component.translatable("origins.gui.choose"), net.minecraft.client.gui.components.Button.OnPress callback@ { b: net.minecraft.client.gui.components.Button -> val current: kotlin.Int = access.`andromeda$getCurrentLayerIndex`()
            if (current < 0 || current >= originLayers.size) return@callback
            val list: java.util.ArrayList<dev.overgrown.origins.origin.OriginLayer> = java.util.ArrayList<dev.overgrown.origins.origin.OriginLayer>()
            list.add(originLayers.get(current).getA())
            net.minecraft.client.Minecraft.getInstance().setScreen(dev.overgrown.origins.client.screen.ChooseOriginScreen(list, 0, false, false))

            }).bounds(0, 0, 100, 20).build())
            access.`andromeda$setChooseOriginButton`(this.`andromeda$chooseButton`)

            if (originLayers.size > 1) {
                this.`andromeda$previousButton` = this.addRenderableWidget<net.minecraft.client.gui.components.Button>(net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.Component.literal("<"), net.minecraft.client.gui.components.Button.OnPress callback@ { b: net.minecraft.client.gui.components.Button -> val current: kotlin.Int = (access.`andromeda$getCurrentLayerIndex`() - 1 + originLayers.size) % originLayers.size
                access.`andromeda$setCurrentLayerIndex`(current)
                val value: net.minecraft.util.Tuple<dev.overgrown.origins.origin.OriginLayer, dev.overgrown.origins.origin.Origin> = originLayers.get(current)
                this.showOrigin(value.getB(), value.getA(), false)
                access.`andromeda$invokeRefreshChooseButton`()
                access.`andromeda$invokeRefreshSwapButton`()

                }).bounds(0, 0, 20, 20).build())
                this.`andromeda$nextButton` = this.addRenderableWidget<net.minecraft.client.gui.components.Button>(net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.Component.literal(">"), net.minecraft.client.gui.components.Button.OnPress callback@ { b: net.minecraft.client.gui.components.Button -> val current: kotlin.Int = (access.`andromeda$getCurrentLayerIndex`() + 1) % originLayers.size
                access.`andromeda$setCurrentLayerIndex`(current)
                val value: net.minecraft.util.Tuple<dev.overgrown.origins.origin.OriginLayer, dev.overgrown.origins.origin.Origin> = originLayers.get(current)
                this.showOrigin(value.getB(), value.getA(), false)
                access.`andromeda$invokeRefreshChooseButton`()
                access.`andromeda$invokeRefreshSwapButton`()

                }).bounds(0, 0, 20, 20).build())
            }
        } else {
            this.`andromeda$chooseButton` = null
        }

        this.`andromeda$closeButton` = this.addRenderableWidget<net.minecraft.client.gui.components.Button>(net.minecraft.client.gui.components.Button.builder(
        net.minecraft.network.chat.Component.translatable("origins.gui.close"), net.minecraft.client.gui.components.Button.OnPress callback@ { b: net.minecraft.client.gui.components.Button -> net.minecraft.client.Minecraft.getInstance().setScreen(null)})
        .bounds(0, 0, 100, 20).build())

        this.`andromeda$swapButton` = this.addRenderableWidget<net.minecraft.client.gui.components.Button>(net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.Component.literal("S"), net.minecraft.client.gui.components.Button.OnPress callback@ { b: net.minecraft.client.gui.components.Button -> val current: kotlin.Int = access.`andromeda$getCurrentLayerIndex`()
        if (current >= 0 && current < originLayers.size) {
            net.minecraft.client.Minecraft.getInstance().setScreen(dev.overgrown.origins.client.screen.SwapOriginScreen(originLayers.get(current).getA().id()))
        }

        }).bounds(0, 0, 20, 20).build())
        this.`andromeda$swapButton`!!.setTooltip(net.minecraft.client.gui.components.Tooltip.create(net.minecraft.network.chat.Component.translatable("origins.gui.swap_origin.button")))
        access.`andromeda$setSwapButton`(this.`andromeda$swapButton`)

        if (this.`andromeda$chooseButton` != null) access.`andromeda$invokeRefreshChooseButton`()
        access.`andromeda$invokeRefreshSwapButton`()

        val choosing: kotlin.Boolean = this.`andromeda$chooseButton` != null && this.`andromeda$chooseButton`!!.visible
        val footerHeight: kotlin.Int = if (choosing) 52 else 29
        this.`andromeda$readerBottom` = this.`andromeda$panelBottom` - footerHeight
        `andromeda$positionButtons`(choosing)
    }

    @org.spongepowered.asm.mixin.Unique private fun `andromeda$positionButtons`(choosing: kotlin.Boolean) {
        val centerX: kotlin.Int = (this.`andromeda$panelLeft` + this.`andromeda$panelRight`) / 2
        if (choosing) {
            val chooseY: kotlin.Int = this.`andromeda$readerBottom` + 6
            val footerY: kotlin.Int = this.`andromeda$readerBottom` + 29
            if (this.`andromeda$chooseButton` != null) this.`andromeda$chooseButton`!!.setPosition(centerX - 50, chooseY)
            if (this.`andromeda$closeButton` != null) this.`andromeda$closeButton`!!.setPosition(centerX - 50, footerY)
            if (this.`andromeda$previousButton` != null) this.`andromeda$previousButton`!!.setPosition(centerX - 79, footerY)
            if (this.`andromeda$nextButton` != null) this.`andromeda$nextButton`!!.setPosition(centerX + 59, footerY)
            if (this.`andromeda$swapButton` != null) this.`andromeda$swapButton`!!.setPosition(centerX + 86, footerY)
        } else {
            val footerY: kotlin.Int = this.`andromeda$readerBottom` + 6
            if (this.`andromeda$closeButton` != null) this.`andromeda$closeButton`!!.setPosition(centerX - 50, footerY)
            if (this.`andromeda$previousButton` != null) this.`andromeda$previousButton`!!.setPosition(centerX - 79, footerY)
            if (this.`andromeda$nextButton` != null) this.`andromeda$nextButton`!!.setPosition(centerX + 59, footerY)
            if (this.`andromeda$swapButton` != null) this.`andromeda$swapButton`!!.setPosition(centerX + 86, footerY)
        }
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = ["render"], at = [org.spongepowered.asm.mixin.injection.At("TAIL")]) private fun `andromeda$renderViewReader`(graphics: net.minecraft.client.gui.GuiGraphics, mouseX: kotlin.Int, mouseY: kotlin.Int, partialTick: kotlin.Float,
    ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfo?) {
        // Visibility can change after a client-state refresh, so keep footer geometry synchronized.
        val choosing: kotlin.Boolean = this.`andromeda$chooseButton` != null && this.`andromeda$chooseButton`!!.visible
        val wantedReaderBottom: kotlin.Int = this.`andromeda$panelBottom` - (if (choosing) 52 else 29)
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

    @org.spongepowered.asm.mixin.Unique private fun `andromeda$renderRichDetails`(graphics: net.minecraft.client.gui.GuiGraphics, mouseX: kotlin.Int, mouseY: kotlin.Int, partialTick: kotlin.Float) {
        val left: kotlin.Int = this.`andromeda$panelLeft`
        val top: kotlin.Int = this.`andromeda$panelTop`
        val right: kotlin.Int = this.`andromeda$panelRight`
        val bottom: kotlin.Int = this.`andromeda$readerBottom`
        if (right <= left || bottom <= top) return

        OriginUiTheme.panel(graphics, left, top, right, this.`andromeda$panelBottom`)
        graphics.fill(left + 1, bottom, right - 1, bottom + 1, OriginUiTheme.DIVIDER)

        val screenTitle: net.minecraft.network.chat.Component = net.minecraft.network.chat.Component.translatable("andromeda_origins.screen.view_origin.title").withStyle(net.minecraft.ChatFormatting.BOLD)
        graphics.drawCenteredString(this.fontRef(), screenTitle, (left + right) / 2, top + 8, OriginUiTheme.TEXT)

        val origin: dev.overgrown.origins.origin.Origin? = this.getCurrentOrigin()
        val layer: dev.overgrown.origins.origin.OriginLayer? = this.getCurrentLayer()
        if (origin == null) {
            val empty: net.minecraft.network.chat.Component = net.minecraft.network.chat.Component.translatable("origins.gui.view_origin.empty").withStyle(net.minecraft.ChatFormatting.GRAY)
            graphics.drawCenteredString(this.fontRef(), empty, (left + right) / 2,
            top + kotlin.math.max(45, (bottom - top) / 2), -0x373738)
            this.setMaxScroll(0)
            return
        }

        if (layer != null) {
            graphics.drawCenteredString(this.fontRef(), layer.name(), (left + right) / 2, top + 21,
            OriginUiTheme.MUTED)
        }

        val readerTop: kotlin.Int = top + 32
        val maxScroll: kotlin.Int = OriginRichReaderRenderer.render(
        graphics,
        this.fontRef(),
        origin,
        net.minecraft.client.Minecraft.getInstance().player,
        OriginRichReaderRenderer.Bounds(left, readerTop, right, bottom),
        this.scrollPos,
        mouseX,
        mouseY,
        partialTick
        )
        this.setMaxScroll(maxScroll)
    }
}
