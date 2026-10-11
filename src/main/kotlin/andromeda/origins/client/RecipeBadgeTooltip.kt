package andromeda.origins.client

import com.mojang.blaze3d.systems.RenderSystem
import dev.overgrown.apoli.client.TooltipRenderer
import dev.overgrown.origins.badge.CraftingRecipeBadge
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

/** Recipe previews use the screen's flat palette without a texture dependency. */
object RecipeBadgeTooltip {
    @JvmStatic
    fun render(graphics: GuiGraphics, font: Font, badge: CraftingRecipeBadge, x: Int, y: Int, width: Int) {
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        val components = mutableListOf<ClientTooltipComponent>()
        fun lines(text: Component) {
            font.split(text, width).forEach { components.add(ClientTooltipComponent.create(it)) }
        }
        badge.prefix().ifPresent { lines(it) }
        if (!badge.output().isEmpty) components.add(object : ClientTooltipComponent {
            override fun getHeight() = 66
            override fun getWidth(font: Font) = 126
            override fun renderImage(font: Font, x: Int, y: Int, graphics: GuiGraphics) {
                fun slot(stack: ItemStack, sx: Int, sy: Int) {
                    graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, OriginUiTheme.BORDER)
                    graphics.fill(sx, sy, sx + 16, sy + 16, OriginUiTheme.PANEL)
                    graphics.renderItem(stack, sx, sy)
                    graphics.renderItemDecorations(font, stack, sx, sy)
                }
                for (row in 0..2) for (column in 0..2) {
                    val index = column + row * badge.width()
                    val stack = if (column < badge.width()) badge.inputs().getOrNull(index) else null
                    slot(stack ?: ItemStack.EMPTY, x + 3 + column * 20, y + 3 + row * 20)
                }
                graphics.drawString(font, Component.literal("→"), x + 70, y + 26, OriginUiTheme.TEXT, false)
                slot(badge.output(), x + 102, y + 23)
            }
        })
        badge.suffix().ifPresent { lines(it) }
        TooltipRenderer.render(graphics, font, components, x, y)
    }
}
