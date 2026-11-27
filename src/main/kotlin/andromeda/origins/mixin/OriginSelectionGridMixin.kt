package andromeda.origins.mixin

import andromeda.origins.client.OriginRichReaderRenderer
import andromeda.origins.client.OriginUiTheme
import andromeda.origins.mixin.ChooseOriginScreenAccessor
import dev.overgrown.apoli.client.IconRenderer
import dev.overgrown.origins.client.screen.ChooseOriginScreen
import dev.overgrown.origins.client.screen.OriginDisplayScreen
import dev.overgrown.origins.network.OriginsClientNetwork
import dev.overgrown.origins.origin.Origin
import dev.overgrown.origins.origin.OriginLayer
import java.util.ArrayList
import kotlin.math.max
import kotlin.math.min
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo


 /**
 * Andromeda's centered flat Origin chooser. Overgrown still owns Origin construction,
 * availability, selection packets, layer flow and the actual Select action; this mixin replaces
 * only presentation/navigation. Random remains disabled on Andromeda's primary layer, but is
 * preserved for independent addon layers so third-party layer semantics remain intact.
 */
@Mixin(ChooseOriginScreen::class)
abstract class OriginSelectionGridMixin protected constructor(title: Component, showDirtBackground: Boolean) : OriginDisplayScreen(title, showDirtBackground) {
    @Unique
    val `andromeda$originButtons`: MutableList<Button> = ArrayList<Button>()
    @Unique
    var `andromeda$page`: Int = 0
    @Unique
    var `andromeda$pages`: Int = 1
    @Unique
    var `andromeda$columns`: Int = 4
    @Unique
    var `andromeda$rows`: Int = 4
    @Unique
    var `andromeda$pageCapacity`: Int = 16
    @Unique
    var `andromeda$panelLeft`: Int = 0
    @Unique
    var `andromeda$panelTop`: Int = 0
    @Unique
    var `andromeda$panelRight`: Int = 0
    @Unique
    var `andromeda$panelBottom`: Int = 0
    @Unique
    var `andromeda$sidebarRight`: Int = 0
    @Unique
    var `andromeda$gridLeft`: Int = 0
    @Unique
    var `andromeda$gridTop`: Int = 0
    @Unique
    var `andromeda$gridWidth`: Int = 0
    @Unique
    var `andromeda$detailLeft`: Int = 0
    @Unique
    var `andromeda$detailTop`: Int = 0
    @Unique
    var `andromeda$detailRight`: Int = 0
    @Unique
    var `andromeda$detailBottom`: Int = 0
    @Unique
    var `andromeda$selectButton`: Button? = null
    @Unique
    var `andromeda$prevPage`: Button? = null
    @Unique
    var `andromeda$nextPage`: Button? = null

    @Inject(method = ["init"], at = [At("TAIL")]) private fun `andromeda$buildGrid`(ci: CallbackInfo?) {
        val access: ChooseOriginScreenAccessor = this as Any as ChooseOriginScreenAccessor
        val origins: MutableList<Origin> = access.`andromeda$getOriginSelection`()
        val originCount: Int = origins.size
        val upstreamMaxSelection: Int = access.`andromeda$getMaxSelection`()
        val layers: ArrayList<OriginLayer> = access.`andromeda$getLayerList`()
        val layerIndex: Int = access.`andromeda$getCurrentLayerIndex`()
        if (layerIndex < 0 || layerIndex >= layers.size) return
        val currentLayer: OriginLayer = layers.get(layerIndex)

                // Andromeda's primary layer intentionally uses the concrete roster. Independent addon
        // layers retain Overgrown's Random entry when their own layer enables it.
        val primaryLayer: Boolean = currentLayer.id() == ResourceLocation.fromNamespaceAndPath("origins", "origin")
        val keepRandom: Boolean = !primaryLayer && upstreamMaxSelection > originCount
        val displayCount: Int = originCount + (if (keepRandom) 1 else 0)
        access.`andromeda$setMaxSelection`(displayCount)

                // The stock screen has finished constructing its controls. Remove those controls and build
        // ours structurally rather than identifying buttons by translated text or dimensions.
        this.clearWidgets()
        if (displayCount <= 0) return
        if (access.`andromeda$getCurrentOriginIndex`() >= displayCount) {
            access.`andromeda$setCurrentOriginIndex`(0)
        }

        val panelWidth: Int = min(760, max(280, this.width - 24))
        this.`andromeda$panelLeft` = (this.width - panelWidth) / 2
        this.`andromeda$panelRight` = this.`andromeda$panelLeft` + panelWidth
        this.`andromeda$panelTop` = max(8, min(14, this.height / 18))
        this.`andromeda$panelBottom` = max(this.`andromeda$panelTop` + 150, this.height - 9)
        this.`andromeda$panelBottom` = min(this.height - 6, this.`andromeda$panelBottom`)

        val sidebarWidth: Int = if (panelWidth >= 500) 154 else if (panelWidth >= 390) 118 else 84
        this.`andromeda$sidebarRight` = this.`andromeda$panelLeft` + sidebarWidth
        this.`andromeda$detailLeft` = this.`andromeda$sidebarRight` + 1
        this.`andromeda$detailTop` = this.`andromeda$panelTop` + 25
        this.`andromeda$detailRight` = this.`andromeda$panelRight`
        this.`andromeda$detailBottom` = this.`andromeda$panelBottom` - 31

        this.`andromeda$selectButton` = this.addRenderableWidget<Button>(Button.builder(
        Component.translatable("origins.gui.select"), Button.OnPress { b: Button -> `andromeda$confirmSelection`()})
        .bounds((this.`andromeda$detailLeft` + this.`andromeda$detailRight`) / 2 - 50,
        this.`andromeda$panelBottom` - 25, 100, 20)
        .build())

        val tile: Int = 30
        val gap: Int = 6
        val innerSidebarWidth: Int = max(32, sidebarWidth - 16)
        this.`andromeda$columns` = max(1, min(4, (innerSidebarWidth + gap) / (tile + gap)))
        val gridAreaTop: Int = this.`andromeda$detailTop` + 25
        val gridAreaBottom: Int = this.`andromeda$detailBottom` - 4
        this.`andromeda$rows` = max(1, (max(tile, gridAreaBottom - gridAreaTop) + gap) / (tile + gap))
        this.`andromeda$pageCapacity` = max(1, this.`andromeda$columns` * this.`andromeda$rows`)
        this.`andromeda$pages` = max(1, (displayCount + this.`andromeda$pageCapacity` - 1) / this.`andromeda$pageCapacity`)
        this.`andromeda$page` = max(0, min(this.`andromeda$pages` - 1,
        access.`andromeda$getCurrentOriginIndex`() / this.`andromeda$pageCapacity`))

        this.`andromeda$gridWidth` = this.`andromeda$columns` * tile + (this.`andromeda$columns` - 1) * gap
        val gridHeight: Int = this.`andromeda$rows` * tile + (this.`andromeda$rows` - 1) * gap
        this.`andromeda$gridLeft` = this.`andromeda$panelLeft` + (sidebarWidth - this.`andromeda$gridWidth`) / 2
        this.`andromeda$gridTop` = gridAreaTop + max(0, (gridAreaBottom - gridAreaTop - gridHeight) / 2)

        this.`andromeda$originButtons`.clear()
        for (index in 0 ..< displayCount) {
            val selectionIndex: Int = index
            val button: Button = Button.builder(Component.empty(), Button.OnPress { b: Button -> `andromeda$select`(selectionIndex)})
            .bounds(0, 0, tile, tile)
            .build()
            this.`andromeda$originButtons`.add(this.addRenderableWidget<Button>(button))
        }

        if (this.`andromeda$pages` > 1) {
            val navY: Int = this.`andromeda$panelBottom` - 25
            this.`andromeda$prevPage` = this.addRenderableWidget<Button>(Button.builder(Component.literal("<"), Button.OnPress { b: Button -> this.`andromeda$page` = (this.`andromeda$page` - 1 + this.`andromeda$pages`) % this.`andromeda$pages`
            `andromeda$updatePage`()

            }).bounds(this.`andromeda$panelLeft` + 10, navY, 22, 20).build())
            this.`andromeda$nextPage` = this.addRenderableWidget<Button>(Button.builder(Component.literal(">"), Button.OnPress { b: Button -> this.`andromeda$page` = (this.`andromeda$page` + 1) % this.`andromeda$pages`
            `andromeda$updatePage`()

            }).bounds(this.`andromeda$sidebarRight` - 32, navY, 22, 20).build())
        } else {
            this.`andromeda$prevPage` = null
            this.`andromeda$nextPage` = null
        }

        `andromeda$updatePage`()
    }

    @Unique private fun `andromeda$select`(index: Int) {
        val access: ChooseOriginScreenAccessor = this as Any as ChooseOriginScreenAccessor
        if (index < 0 || index >= access.`andromeda$getMaxSelection`()) return
        access.`andromeda$setCurrentOriginIndex`(index)
        val selected: Origin? = access.`andromeda$invokeGetCurrentOriginInternal`()
        val layers: ArrayList<OriginLayer> = access.`andromeda$getLayerList`()
        val layerIndex: Int = access.`andromeda$getCurrentLayerIndex`()
        if (selected == null || layerIndex < 0 || layerIndex >= layers.size) return
        this.showOrigin(selected, layers.get(layerIndex), index == access.`andromeda$getOriginSelection`().size)
    }

    @Unique private fun `andromeda$confirmSelection`() {
        val access: ChooseOriginScreenAccessor = this as Any as ChooseOriginScreenAccessor
        val layers: ArrayList<OriginLayer> = access.`andromeda$getLayerList`()
        val layerIndex: Int = access.`andromeda$getCurrentLayerIndex`()
        val index: Int = access.`andromeda$getCurrentOriginIndex`()
        if (layerIndex < 0 || layerIndex >= layers.size || index < 0 || index >= access.`andromeda$getMaxSelection`()) return

        val layer: OriginLayer = layers.get(layerIndex)
        val originCount: Int = access.`andromeda$getOriginSelection`().size
        val originId: ResourceLocation?
        if (index == originCount) {
            originId = ResourceLocation.fromNamespaceAndPath("origins", "random")
        } else {
            val selected: Origin? = access.`andromeda$invokeGetCurrentOriginInternal`()
            if (selected == null) return
            originId = selected.id()
        }
        OriginsClientNetwork.sendChoose(layer.id(), originId, access.`andromeda$isFromOrb`())
        access.`andromeda$invokeOpenNextLayerScreen`()
    }

    @Unique private fun `andromeda$updatePage`() {
        val start: Int = this.`andromeda$page` * this.`andromeda$pageCapacity`
        val end: Int = start + this.`andromeda$pageCapacity`
        val tile: Int = 30
        val gap: Int = 6
        for (i in this.`andromeda$originButtons`.indices) {
            val button: Button = this.`andromeda$originButtons`.get(i)
            val visible: Boolean = i >= start && i < end
            button.visible = visible
            button.active = visible
            if (!visible) continue
            val local: Int = i - start
            val col: Int = local % this.`andromeda$columns`
            val row: Int = local / this.`andromeda$columns`
            button.setPosition(this.`andromeda$gridLeft` + col * (tile + gap),
            this.`andromeda$gridTop` + row * (tile + gap))
        }
    }

    @Inject(method = ["render"], at = [At("TAIL")]) private fun `andromeda$renderGridAndReader`(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float,
    ci: CallbackInfo?) {
        `andromeda$renderUnifiedPanel`(graphics, mouseX, mouseY, partialTick)

        val access: ChooseOriginScreenAccessor = this as Any as ChooseOriginScreenAccessor
        val origins: MutableList<Origin> = access.`andromeda$getOriginSelection`()
        val selectedIndex: Int = access.`andromeda$getCurrentOriginIndex`()
        val start: Int = this.`andromeda$page` * this.`andromeda$pageCapacity`
        val end: Int = min(this.`andromeda$originButtons`.size, start + this.`andromeda$pageCapacity`)

        var hovered: Component? = null
        for (i in start ..< end) {
            val button: Button = this.`andromeda$originButtons`.get(i)
            if (!button.visible) continue
            val x: Int = button.getX()
            val y: Int = button.getY()

            OriginUiTheme.flatButton(graphics, this.fontRef(), button, mouseX, mouseY, i == selectedIndex)
            if (i < origins.size) {
                val origin: Origin = origins.get(i)
                IconRenderer.render(graphics, origin.icon(), x + 7, y + 7)
                if (origin.hasTag("andromeda_origins:champion")) {
                    graphics.drawString(this.fontRef(), "C", x + 21, y + 20, -0x333334, false)
                }
                if (mouseX >= x && mouseX < x + 30 && mouseY >= y && mouseY < y + 30) hovered = origin.name()
            } else {
                graphics.renderItem(ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("origins", "orb_of_origin"))), x + 7, y + 7)
                if (mouseX >= x && mouseX < x + 30 && mouseY >= y && mouseY < y + 30) {
                    hovered = Component.translatable("origin.origins.random.name")
                }
            }
        }

        OriginUiTheme.flatButton(graphics, this.fontRef(), this.`andromeda$selectButton`, mouseX, mouseY, false)
        OriginUiTheme.flatButton(graphics, this.fontRef(), this.`andromeda$prevPage`, mouseX, mouseY, false)
        OriginUiTheme.flatButton(graphics, this.fontRef(), this.`andromeda$nextPage`, mouseX, mouseY, false)

        if (this.`andromeda$pages` > 1) {
            graphics.drawCenteredString(this.fontRef(),
            Component.literal((this.`andromeda$page` + 1).toString() + "/" + this.`andromeda$pages`),
            (this.`andromeda$panelLeft` + this.`andromeda$sidebarRight`) / 2,
            this.`andromeda$panelBottom` - 19, OriginUiTheme.MUTED)
        }
        if (hovered != null) {
            graphics.renderTooltip(this.fontRef(), hovered, mouseX, mouseY)
        }
    }

    @Unique private fun `andromeda$renderUnifiedPanel`(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
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

        val title: Component = this.getTitleText().copy().withStyle(ChatFormatting.BOLD)
        graphics.drawCenteredString(this.fontRef(), title,
        (this.`andromeda$panelLeft` + this.`andromeda$panelRight`) / 2,
        this.`andromeda$panelTop` + 8, OriginUiTheme.TEXT)

        val layer: OriginLayer? = this.getCurrentLayer()
        if (layer != null) {
            graphics.drawCenteredString(this.fontRef(), layer.name(),
            (this.`andromeda$panelLeft` + this.`andromeda$sidebarRight`) / 2,
            this.`andromeda$detailTop` + 6, OriginUiTheme.MUTED)
        }

        `andromeda$renderRichDetails`(graphics, mouseX, mouseY, partialTick)
    }

    @Unique private fun `andromeda$renderRichDetails`(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val origin: Origin? = this.getCurrentOrigin()
        if (origin == null) {
            this.setMaxScroll(0)
            return
        }
        val maxScroll: Int = OriginRichReaderRenderer.render(
        graphics,
        this.fontRef(),
        origin,
        Minecraft.getInstance().player,
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
