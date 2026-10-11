package andromeda.origins.mixin

import andromeda.origins.client.OriginRichReaderRenderer
import andromeda.origins.client.OriginUiTheme
import andromeda.origins.mixin.ChooseOriginScreenAccessor


 /**
 * Andromeda's centered flat Origin chooser. Overgrown still owns Origin construction,
 * availability, selection packets, layer flow and the actual Select action; this mixin replaces
 * only presentation/navigation. Random remains disabled on Andromeda's primary layer, but is
 * preserved for independent addon layers so third-party layer semantics remain intact.
 */
@org.spongepowered.asm.mixin.Mixin(dev.overgrown.origins.client.screen.ChooseOriginScreen::class)
abstract class OriginSelectionGridMixin protected constructor(title: net.minecraft.network.chat.Component, showDirtBackground: kotlin.Boolean) : dev.overgrown.origins.client.screen.OriginDisplayScreen(title, showDirtBackground) {
    @org.spongepowered.asm.mixin.Unique
    val `andromeda$originButtons`: kotlin.collections.MutableList<net.minecraft.client.gui.components.Button> = java.util.ArrayList<net.minecraft.client.gui.components.Button>()
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$page`: kotlin.Int = 0
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$pages`: kotlin.Int = 1
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$columns`: kotlin.Int = 4
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$rows`: kotlin.Int = 4
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$pageCapacity`: kotlin.Int = 16
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$panelLeft`: kotlin.Int = 0
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$panelTop`: kotlin.Int = 0
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$panelRight`: kotlin.Int = 0
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$panelBottom`: kotlin.Int = 0
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$sidebarRight`: kotlin.Int = 0
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$gridLeft`: kotlin.Int = 0
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$gridTop`: kotlin.Int = 0
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$gridWidth`: kotlin.Int = 0
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$detailLeft`: kotlin.Int = 0
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$detailTop`: kotlin.Int = 0
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$detailRight`: kotlin.Int = 0
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$detailBottom`: kotlin.Int = 0
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$selectButton`: net.minecraft.client.gui.components.Button? = null
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$prevPage`: net.minecraft.client.gui.components.Button? = null
    @org.spongepowered.asm.mixin.Unique
    var `andromeda$nextPage`: net.minecraft.client.gui.components.Button? = null

    @org.spongepowered.asm.mixin.injection.Inject(method = ["init"], at = [org.spongepowered.asm.mixin.injection.At("TAIL")]) private fun `andromeda$buildGrid`(ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfo?) {
        val access: ChooseOriginScreenAccessor = this as kotlin.Any as ChooseOriginScreenAccessor
        val origins: kotlin.collections.MutableList<dev.overgrown.origins.origin.Origin> = access.`andromeda$getOriginSelection`()
        val originCount: kotlin.Int = origins.size
        val upstreamMaxSelection: kotlin.Int = access.`andromeda$getMaxSelection`()
        val layers: java.util.ArrayList<dev.overgrown.origins.origin.OriginLayer> = access.`andromeda$getLayerList`()
        val layerIndex: kotlin.Int = access.`andromeda$getCurrentLayerIndex`()
        if (layerIndex < 0 || layerIndex >= layers.size) return
        val currentLayer: dev.overgrown.origins.origin.OriginLayer = layers.get(layerIndex)

                // Andromeda's primary layer intentionally uses the concrete roster. Independent addon
        // layers retain Overgrown's Random entry when their own layer enables it.
        val primaryLayer: kotlin.Boolean = currentLayer.id() == net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("origins", "origin")
        val keepRandom: kotlin.Boolean = !primaryLayer && upstreamMaxSelection > originCount
        val displayCount: kotlin.Int = originCount + (if (keepRandom) 1 else 0)
        access.`andromeda$setMaxSelection`(displayCount)

                // The stock screen has finished constructing its controls. Remove those controls and build
        // ours structurally rather than identifying buttons by translated text or dimensions.
        this.clearWidgets()
        if (displayCount <= 0) return
        if (access.`andromeda$getCurrentOriginIndex`() >= displayCount) {
            access.`andromeda$setCurrentOriginIndex`(0)
        }

        val panelWidth: kotlin.Int = kotlin.math.min(760, kotlin.math.max(280, this.width - 24))
        this.`andromeda$panelLeft` = (this.width - panelWidth) / 2
        this.`andromeda$panelRight` = this.`andromeda$panelLeft` + panelWidth
        this.`andromeda$panelTop` = kotlin.math.max(8, kotlin.math.min(14, this.height / 18))
        this.`andromeda$panelBottom` = kotlin.math.max(this.`andromeda$panelTop` + 150, this.height - 9)
        this.`andromeda$panelBottom` = kotlin.math.min(this.height - 6, this.`andromeda$panelBottom`)

        val sidebarWidth: kotlin.Int = if (panelWidth >= 500) 154 else if (panelWidth >= 390) 118 else 84
        this.`andromeda$sidebarRight` = this.`andromeda$panelLeft` + sidebarWidth
        this.`andromeda$detailLeft` = this.`andromeda$sidebarRight` + 1
        this.`andromeda$detailTop` = this.`andromeda$panelTop` + 25
        this.`andromeda$detailRight` = this.`andromeda$panelRight`
        this.`andromeda$detailBottom` = this.`andromeda$panelBottom` - 31

        this.`andromeda$selectButton` = this.addRenderableWidget<net.minecraft.client.gui.components.Button>(net.minecraft.client.gui.components.Button.builder(
        net.minecraft.network.chat.Component.translatable("origins.gui.select"), net.minecraft.client.gui.components.Button.OnPress { b: net.minecraft.client.gui.components.Button -> `andromeda$confirmSelection`()})
        .bounds((this.`andromeda$detailLeft` + this.`andromeda$detailRight`) / 2 - 50,
        this.`andromeda$panelBottom` - 25, 100, 20)
        .build())

        val tile: kotlin.Int = 30
        val gap: kotlin.Int = 6
        val innerSidebarWidth: kotlin.Int = kotlin.math.max(32, sidebarWidth - 16)
        this.`andromeda$columns` = kotlin.math.max(1, kotlin.math.min(4, (innerSidebarWidth + gap) / (tile + gap)))
        val gridAreaTop: kotlin.Int = this.`andromeda$detailTop` + 25
        val gridAreaBottom: kotlin.Int = this.`andromeda$detailBottom` - 4
        this.`andromeda$rows` = kotlin.math.max(1, (kotlin.math.max(tile, gridAreaBottom - gridAreaTop) + gap) / (tile + gap))
        this.`andromeda$pageCapacity` = kotlin.math.max(1, this.`andromeda$columns` * this.`andromeda$rows`)
        this.`andromeda$pages` = kotlin.math.max(1, (displayCount + this.`andromeda$pageCapacity` - 1) / this.`andromeda$pageCapacity`)
        this.`andromeda$page` = kotlin.math.max(0, kotlin.math.min(this.`andromeda$pages` - 1,
        access.`andromeda$getCurrentOriginIndex`() / this.`andromeda$pageCapacity`))

        this.`andromeda$gridWidth` = this.`andromeda$columns` * tile + (this.`andromeda$columns` - 1) * gap
        val gridHeight: kotlin.Int = this.`andromeda$rows` * tile + (this.`andromeda$rows` - 1) * gap
        this.`andromeda$gridLeft` = this.`andromeda$panelLeft` + (sidebarWidth - this.`andromeda$gridWidth`) / 2
        this.`andromeda$gridTop` = gridAreaTop + kotlin.math.max(0, (gridAreaBottom - gridAreaTop - gridHeight) / 2)

        this.`andromeda$originButtons`.clear()
        for (index in 0 ..< displayCount) {
            val selectionIndex: kotlin.Int = index
            val button: net.minecraft.client.gui.components.Button = net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.Component.empty(), net.minecraft.client.gui.components.Button.OnPress { b: net.minecraft.client.gui.components.Button -> `andromeda$select`(selectionIndex)})
            .bounds(0, 0, tile, tile)
            .build()
            this.`andromeda$originButtons`.add(this.addRenderableWidget<net.minecraft.client.gui.components.Button>(button))
        }

        if (this.`andromeda$pages` > 1) {
            val navY: kotlin.Int = this.`andromeda$panelBottom` - 25
            this.`andromeda$prevPage` = this.addRenderableWidget<net.minecraft.client.gui.components.Button>(net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.Component.literal("<"), net.minecraft.client.gui.components.Button.OnPress { b: net.minecraft.client.gui.components.Button -> this.`andromeda$page` = (this.`andromeda$page` - 1 + this.`andromeda$pages`) % this.`andromeda$pages`
            `andromeda$updatePage`()

            }).bounds(this.`andromeda$panelLeft` + 10, navY, 22, 20).build())
            this.`andromeda$nextPage` = this.addRenderableWidget<net.minecraft.client.gui.components.Button>(net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.Component.literal(">"), net.minecraft.client.gui.components.Button.OnPress { b: net.minecraft.client.gui.components.Button -> this.`andromeda$page` = (this.`andromeda$page` + 1) % this.`andromeda$pages`
            `andromeda$updatePage`()

            }).bounds(this.`andromeda$sidebarRight` - 32, navY, 22, 20).build())
        } else {
            this.`andromeda$prevPage` = null
            this.`andromeda$nextPage` = null
        }

        `andromeda$updatePage`()
    }

    @org.spongepowered.asm.mixin.Unique private fun `andromeda$select`(index: kotlin.Int) {
        val access: ChooseOriginScreenAccessor = this as kotlin.Any as ChooseOriginScreenAccessor
        if (index < 0 || index >= access.`andromeda$getMaxSelection`()) return
        access.`andromeda$setCurrentOriginIndex`(index)
        val selected: dev.overgrown.origins.origin.Origin? = access.`andromeda$invokeGetCurrentOriginInternal`()
        val layers: java.util.ArrayList<dev.overgrown.origins.origin.OriginLayer> = access.`andromeda$getLayerList`()
        val layerIndex: kotlin.Int = access.`andromeda$getCurrentLayerIndex`()
        if (selected == null || layerIndex < 0 || layerIndex >= layers.size) return
        this.showOrigin(selected, layers.get(layerIndex), index == access.`andromeda$getOriginSelection`().size)
    }

    @org.spongepowered.asm.mixin.Unique private fun `andromeda$confirmSelection`() {
        val access: ChooseOriginScreenAccessor = this as kotlin.Any as ChooseOriginScreenAccessor
        val layers: java.util.ArrayList<dev.overgrown.origins.origin.OriginLayer> = access.`andromeda$getLayerList`()
        val layerIndex: kotlin.Int = access.`andromeda$getCurrentLayerIndex`()
        val index: kotlin.Int = access.`andromeda$getCurrentOriginIndex`()
        if (layerIndex < 0 || layerIndex >= layers.size || index < 0 || index >= access.`andromeda$getMaxSelection`()) return

        val layer: dev.overgrown.origins.origin.OriginLayer = layers.get(layerIndex)
        val originCount: kotlin.Int = access.`andromeda$getOriginSelection`().size
        val originId: net.minecraft.resources.ResourceLocation?
        if (index == originCount) {
            originId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("origins", "random")
        } else {
            val selected: dev.overgrown.origins.origin.Origin? = access.`andromeda$invokeGetCurrentOriginInternal`()
            if (selected == null) return
            originId = selected.id()
        }
        dev.overgrown.origins.network.OriginsClientNetwork.sendChoose(layer.id(), originId, access.`andromeda$isFromOrb`())
        access.`andromeda$invokeOpenNextLayerScreen`()
    }

    @org.spongepowered.asm.mixin.Unique private fun `andromeda$updatePage`() {
        val start: kotlin.Int = this.`andromeda$page` * this.`andromeda$pageCapacity`
        val end: kotlin.Int = start + this.`andromeda$pageCapacity`
        val tile: kotlin.Int = 30
        val gap: kotlin.Int = 6
        for (i in this.`andromeda$originButtons`.indices) {
            val button: net.minecraft.client.gui.components.Button = this.`andromeda$originButtons`.get(i)
            val visible: kotlin.Boolean = i >= start && i < end
            button.visible = visible
            button.active = visible
            if (!visible) continue
            val local: kotlin.Int = i - start
            val col: kotlin.Int = local % this.`andromeda$columns`
            val row: kotlin.Int = local / this.`andromeda$columns`
            button.setPosition(this.`andromeda$gridLeft` + col * (tile + gap),
            this.`andromeda$gridTop` + row * (tile + gap))
        }
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = ["render"], at = [org.spongepowered.asm.mixin.injection.At("TAIL")]) private fun `andromeda$renderGridAndReader`(graphics: net.minecraft.client.gui.GuiGraphics, mouseX: kotlin.Int, mouseY: kotlin.Int, partialTick: kotlin.Float,
    ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfo?) {
        `andromeda$renderUnifiedPanel`(graphics, mouseX, mouseY, partialTick)

        val access: ChooseOriginScreenAccessor = this as kotlin.Any as ChooseOriginScreenAccessor
        val origins: kotlin.collections.MutableList<dev.overgrown.origins.origin.Origin> = access.`andromeda$getOriginSelection`()
        val selectedIndex: kotlin.Int = access.`andromeda$getCurrentOriginIndex`()
        val start: kotlin.Int = this.`andromeda$page` * this.`andromeda$pageCapacity`
        val end: kotlin.Int = kotlin.math.min(this.`andromeda$originButtons`.size, start + this.`andromeda$pageCapacity`)

        var hovered: net.minecraft.network.chat.Component? = null
        for (i in start ..< end) {
            val button: net.minecraft.client.gui.components.Button = this.`andromeda$originButtons`.get(i)
            if (!button.visible) continue
            val x: kotlin.Int = button.getX()
            val y: kotlin.Int = button.getY()

            OriginUiTheme.flatButton(graphics, this.fontRef(), button, mouseX, mouseY, i == selectedIndex)
            if (i < origins.size) {
                val origin: dev.overgrown.origins.origin.Origin = origins.get(i)
                dev.overgrown.apoli.client.IconRenderer.render(graphics, origin.icon(), x + 7, y + 7)
                if (origin.hasTag("andromeda_origins:champion")) {
                    graphics.drawString(this.fontRef(), "C", x + 21, y + 20, -0x333334, false)
                }
                if (mouseX >= x && mouseX < x + 30 && mouseY >= y && mouseY < y + 30) hovered = origin.name()
            } else {
                graphics.renderItem(net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("origins", "orb_of_origin"))), x + 7, y + 7)
                if (mouseX >= x && mouseX < x + 30 && mouseY >= y && mouseY < y + 30) {
                    hovered = net.minecraft.network.chat.Component.translatable("origin.origins.random.name")
                }
            }
        }

        OriginUiTheme.flatButton(graphics, this.fontRef(), this.`andromeda$selectButton`, mouseX, mouseY, false)
        OriginUiTheme.flatButton(graphics, this.fontRef(), this.`andromeda$prevPage`, mouseX, mouseY, false)
        OriginUiTheme.flatButton(graphics, this.fontRef(), this.`andromeda$nextPage`, mouseX, mouseY, false)

        if (this.`andromeda$pages` > 1) {
            graphics.drawCenteredString(this.fontRef(),
            net.minecraft.network.chat.Component.literal((this.`andromeda$page` + 1).toString() + "/" + this.`andromeda$pages`),
            (this.`andromeda$panelLeft` + this.`andromeda$sidebarRight`) / 2,
            this.`andromeda$panelBottom` - 19, OriginUiTheme.MUTED)
        }
        if (hovered != null) {
            graphics.renderTooltip(this.fontRef(), hovered, mouseX, mouseY)
        }
    }

    @org.spongepowered.asm.mixin.Unique private fun `andromeda$renderUnifiedPanel`(graphics: net.minecraft.client.gui.GuiGraphics, mouseX: kotlin.Int, mouseY: kotlin.Int, partialTick: kotlin.Float) {
        if (this.`andromeda$panelRight` <= this.`andromeda$panelLeft`
        || this.`andromeda$panelBottom` <= this.`andromeda$panelTop`) return

        OriginUiTheme.panel(graphics, this.`andromeda$panelLeft`, this.`andromeda$panelTop`,
        this.`andromeda$panelRight`, this.`andromeda$panelBottom`)
        graphics.fill(this.`andromeda$panelLeft` + 1, this.`andromeda$panelTop` + 1,
        this.`andromeda$sidebarRight`, this.`andromeda$panelBottom` - 1, OriginUiTheme.SIDEBAR)
        graphics.fill(this.`andromeda$sidebarRight`, this.`andromeda$panelTop` + 1,
        this.`andromeda$sidebarRight` + 1, this.`andromeda$panelBottom` - 1, OriginUiTheme.DIVIDER)
        graphics.fill(this.`andromeda$detailLeft`, this.`andromeda$panelBottom` - 31,
        this.`andromeda$panelRight` - 1, this.`andromeda$panelBottom` - 30, OriginUiTheme.DIVIDER)

        val title: net.minecraft.network.chat.Component = this.getTitleText().copy().withStyle(net.minecraft.ChatFormatting.BOLD)
        graphics.drawCenteredString(this.fontRef(), title,
        (this.`andromeda$panelLeft` + this.`andromeda$panelRight`) / 2,
        this.`andromeda$panelTop` + 8, OriginUiTheme.TEXT)

        val layer: dev.overgrown.origins.origin.OriginLayer? = this.getCurrentLayer()
        if (layer != null) {
            graphics.drawCenteredString(this.fontRef(), layer.name(),
            (this.`andromeda$panelLeft` + this.`andromeda$sidebarRight`) / 2,
            this.`andromeda$detailTop` + 6, OriginUiTheme.MUTED)
        }

        `andromeda$renderRichDetails`(graphics, mouseX, mouseY, partialTick)
    }

    @org.spongepowered.asm.mixin.Unique private fun `andromeda$renderRichDetails`(graphics: net.minecraft.client.gui.GuiGraphics, mouseX: kotlin.Int, mouseY: kotlin.Int, partialTick: kotlin.Float) {
        val origin: dev.overgrown.origins.origin.Origin? = this.getCurrentOrigin()
        if (origin == null) {
            this.setMaxScroll(0)
            return
        }
        val maxScroll: kotlin.Int = OriginRichReaderRenderer.render(
        graphics,
        this.fontRef(),
        origin,
        net.minecraft.client.Minecraft.getInstance().player,
        OriginRichReaderRenderer.Bounds(
        this.`andromeda$detailLeft`,
        this.`andromeda$detailTop`,
        this.`andromeda$detailRight`,
        this.`andromeda$detailBottom`
        ),
        this.scrollPos,
        mouseX,
        mouseY,
        partialTick
        )
        this.setMaxScroll(maxScroll)
    }

}
