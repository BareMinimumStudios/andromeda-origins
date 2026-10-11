package andromeda.origins.client

import andromeda.origins.client.OriginUiTheme
import andromeda.origins.client.RecipeBadgeTooltip


 /**
 * Shared rich-text Origin document renderer used by both the chooser and the O-key self viewer.
 *
 *
 * This class deliberately owns only presentation. Origin state, power visibility, selection,
 * networking, scrolling input and layer flow remain owned by Overgrown's screens/mixins.
 */
object OriginRichReaderRenderer {
    private const val LINE_HEIGHT: kotlin.Int = 11

     /** Renders the current Origin document and returns the maximum valid scroll offset.  */
    fun render(graphics: net.minecraft.client.gui.GuiGraphics, font: net.minecraft.client.gui.Font, origin: dev.overgrown.origins.origin.Origin?, viewer: net.minecraft.world.entity.player.Player?,
    bounds: OriginRichReaderRenderer.Bounds, scrollPos: kotlin.Int, mouseX: kotlin.Int, mouseY: kotlin.Int, partialTick: kotlin.Float): kotlin.Int {
        if (origin == null || bounds.width() <= 0 || bounds.height() <= 0) return 0

        val left: kotlin.Int = bounds.left
        val top: kotlin.Int = bounds.top
        val right: kotlin.Int = bounds.right
        val bottom: kotlin.Int = bounds.bottom

        val headerY: kotlin.Int = top + 5
        dev.overgrown.apoli.client.IconRenderer.render(graphics, origin.icon(), left + 13, headerY + 1)
        val originName: net.minecraft.network.chat.MutableComponent = origin.name().copy().withStyle(net.minecraft.ChatFormatting.BOLD)
        graphics.drawString(font, originName, left + 37, headerY + 1, OriginUiTheme.TEXT, false)
        val impactName: net.minecraft.network.chat.Component = net.minecraft.network.chat.Component.translatable("origins.gui.impact." + origin.impact().name.lowercase())
        val impact: net.minecraft.network.chat.Component = net.minecraft.network.chat.Component.translatable("andromeda_origins.screen.reader.impact", impactName).withStyle(net.minecraft.ChatFormatting.GRAY)
        graphics.drawString(font, impact,
        kotlin.math.max(left + 37, right - 14 - font.width(impact)),
        headerY + 1, OriginUiTheme.MUTED, false)
        graphics.fill(left + 12, headerY + 20, right - 12, headerY + 21, OriginUiTheme.DIVIDER)

        val contentLeft: kotlin.Int = left + 15
        val contentRight: kotlin.Int = right - 16
        val contentTop: kotlin.Int = headerY + 29
        val contentBottom: kotlin.Int = bottom - 8
        val textWidth: kotlin.Int = kotlin.math.max(90, contentRight - contentLeft - 8)
        var y: kotlin.Int = contentTop - scrollPos
        var hoveredBadge: dev.overgrown.origins.badge.Badge? = null
        var hoveredBadgePowerId: net.minecraft.resources.ResourceLocation? = null

        graphics.enableScissor(contentLeft - 2, contentTop, contentRight + 2, contentBottom)

        y = OriginRichReaderRenderer.drawSection(graphics, font, net.minecraft.network.chat.Component.translatable("andromeda_origins.screen.reader.overview"), y, contentLeft, textWidth)
        y = OriginRichReaderRenderer.drawWrapped(graphics, font, origin.description(), y, contentLeft + 2,
        textWidth - 4, OriginUiTheme.BODY, OriginRichReaderRenderer.LINE_HEIGHT)
        y += 10

        y = OriginRichReaderRenderer.drawSection(graphics, font, net.minecraft.network.chat.Component.translatable("andromeda_origins.screen.reader.powers_traits"), y, contentLeft, textWidth)
        val shownPowers: kotlin.collections.MutableList<net.minecraft.resources.ResourceLocation> = if (viewer != null) origin.powersFor(viewer) else origin.powers()
        for (powerId in shownPowers) {
            val power: dev.overgrown.apoli.power.Power? = dev.overgrown.apoli.power.ApoliPowers.get(powerId)
            if (power == null || power.hidden()) continue

            val powerName: net.minecraft.network.chat.MutableComponent = net.minecraft.network.chat.Component.literal("• ")
            .withStyle(net.minecraft.ChatFormatting.GRAY)
            .append(power.displayName(powerId).copy().withStyle(net.minecraft.ChatFormatting.BOLD, net.minecraft.ChatFormatting.WHITE))
            y = OriginRichReaderRenderer.drawWrapped(graphics, font, powerName, y, contentLeft, textWidth,
            OriginUiTheme.TEXT, OriginRichReaderRenderer.LINE_HEIGHT)

            val badges: kotlin.collections.MutableList<dev.overgrown.origins.badge.Badge> = dev.overgrown.origins.client.BadgeClientState.get(powerId)
            if (!badges.isEmpty()) {
                val row: BadgeRow = OriginRichReaderRenderer.drawBadges(graphics, badges, powerId, contentLeft + 7, contentLeft + textWidth,
                y, mouseX, mouseY, contentTop, contentBottom)
                y = row.nextY
                if (row.hovered != null) {
                    hoveredBadge = row.hovered
                    hoveredBadgePowerId = row.powerId
                }
            } else {
                y += 2
            }

            y = OriginRichReaderRenderer.drawWrapped(graphics, font, power.displayDescription(powerId), y,
            contentLeft + 7, textWidth - 7, -0x333334, OriginRichReaderRenderer.LINE_HEIGHT)
            y += 9
        }

        graphics.disableScissor()

        val fullHeight: kotlin.Int = (y + scrollPos) - contentTop
        val viewport: kotlin.Int = kotlin.math.max(1, contentBottom - contentTop)
        val maxScroll: kotlin.Int = kotlin.math.max(0, fullHeight - viewport)
        OriginRichReaderRenderer.renderScrollbar(graphics, right, contentTop, contentBottom, scrollPos, maxScroll, fullHeight, viewport)

        if (hoveredBadge != null && hoveredBadge.hasTooltip()) {
            val widthLimit: kotlin.Int = kotlin.math.max(120, bounds.right - mouseX - 18)
            com.mojang.blaze3d.systems.RenderSystem.enableBlend()
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc()
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            if (hoveredBadge is dev.overgrown.origins.badge.CraftingRecipeBadge) {
                RecipeBadgeTooltip.render(graphics, font, hoveredBadge, mouseX, mouseY, widthLimit)
            } else {
                hoveredBadge.renderTooltip(graphics, font, mouseX, mouseY, widthLimit, hoveredBadgePowerId, partialTick)
            }
        }
        return maxScroll
    }

    private fun drawBadges(graphics: net.minecraft.client.gui.GuiGraphics, badges: kotlin.collections.MutableList<dev.overgrown.origins.badge.Badge>, powerId: net.minecraft.resources.ResourceLocation,
    startX: kotlin.Int, right: kotlin.Int, y: kotlin.Int, mouseX: kotlin.Int, mouseY: kotlin.Int,
    clipTop: kotlin.Int, clipBottom: kotlin.Int): BadgeRow {
        val size: kotlin.Int = 9
        val step: kotlin.Int = 11
        var x: kotlin.Int = startX
        var rowY: kotlin.Int = y
        var hovered: dev.overgrown.origins.badge.Badge? = null

        for (badge in badges) {
            if (x + size > right) {
                x = startX
                rowY += step
            }
            if (rowY >= clipTop - size && rowY <= clipBottom + size) {
                val sprite: net.minecraft.resources.ResourceLocation = OriginRichReaderRenderer.resolveBadgeSprite(badge.spriteId())
                val spriteSize: kotlin.Int = OriginRichReaderRenderer.badgeSpriteSize(sprite)
                com.mojang.blaze3d.systems.RenderSystem.enableBlend()
                com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc()
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
                graphics.blit(sprite, x, rowY, size, size,
                0.0f, 0.0f, spriteSize, spriteSize, spriteSize, spriteSize)
                if (badge.hasTooltip() && mouseX >= x && mouseX < x + size && mouseY >= kotlin.math.max(rowY, clipTop) && mouseY < kotlin.math.min(rowY + size, clipBottom)) {
                    hovered = badge
                }
            }
            x += step
        }
        return BadgeRow(rowY + step + 1, hovered, if (hovered == null) null else powerId)
    }

    private fun resolveBadgeSprite(requested: net.minecraft.resources.ResourceLocation): net.minecraft.resources.ResourceLocation {
        val minecraft: net.minecraft.client.Minecraft = net.minecraft.client.Minecraft.getInstance()
        if (minecraft.getResourceManager().getResource(requested).isPresent()) return requested

        // Native badge artwork stays in its dependency. Missing resources use our own
        // informational glyph while retaining the badge's full tooltip and behavior.
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
            "andromeda_origins", "textures/gui/badge/fallback.png")
    }

    private fun badgeSpriteSize(sprite: net.minecraft.resources.ResourceLocation?): kotlin.Int {
        // BadgeClientState caches dimensions by id, and works for both Origins' sprite namespace
        // and our independently drawn fallback glyph.
        return dev.overgrown.origins.client.BadgeClientState.spriteSize(sprite)
    }

    private fun drawSection(graphics: net.minecraft.client.gui.GuiGraphics, font: net.minecraft.client.gui.Font, label: net.minecraft.network.chat.Component, y: kotlin.Int, x: kotlin.Int, width: kotlin.Int): kotlin.Int {
        val heading: net.minecraft.network.chat.MutableComponent = label.copy().withStyle(net.minecraft.ChatFormatting.BOLD, net.minecraft.ChatFormatting.WHITE)
        graphics.drawString(font, heading, x, y, -0x1f1f20, false)
        val lineX: kotlin.Int = x + font.width(heading) + 7
        if (lineX < x + width) {
            graphics.fill(lineX, y + 5, x + width, y + 6, OriginUiTheme.DIVIDER)
        }
        return y + 15
    }

    private fun drawWrapped(graphics: net.minecraft.client.gui.GuiGraphics, font: net.minecraft.client.gui.Font, component: net.minecraft.network.chat.Component, y: kotlin.Int, x: kotlin.Int,
    width: kotlin.Int, fallbackColor: kotlin.Int, lineHeight: kotlin.Int): kotlin.Int {
        var y: kotlin.Int = y
        for (line in font.split(component, kotlin.math.max(40, width))) {
            graphics.drawString(font, line, x, y, fallbackColor, false)
            y += lineHeight
        }
        return y
    }

    private fun renderScrollbar(graphics: net.minecraft.client.gui.GuiGraphics, right: kotlin.Int, trackTop: kotlin.Int, trackBottom: kotlin.Int,
    scrollPos: kotlin.Int, maxScroll: kotlin.Int, fullHeight: kotlin.Int, viewport: kotlin.Int) {
        if (maxScroll <= 0) return
        val trackX: kotlin.Int = right - 8
        graphics.fill(trackX, trackTop, trackX + 3, trackBottom, OriginUiTheme.SCROLL_TRACK)
        val trackHeight: kotlin.Int = trackBottom - trackTop
        val thumbHeight: kotlin.Int = kotlin.math.max(18,
        ((viewport / kotlin.math.max(viewport, fullHeight).toFloat()) * trackHeight).toInt())
        val travel: kotlin.Int = kotlin.math.max(1, trackHeight - thumbHeight)
        val thumbY: kotlin.Int = trackTop + ((scrollPos / maxScroll.toFloat()) * travel).toInt()
        graphics.fill(trackX - 1, thumbY, trackX + 4, thumbY + thumbHeight, OriginUiTheme.SCROLL_THUMB)
    }

    data class Bounds (val left: kotlin.Int, val top: kotlin.Int, val right: kotlin.Int, val bottom: kotlin.Int) {
        fun width(): kotlin.Int {
            return kotlin.math.max(0, right - left)
        }

        fun height(): kotlin.Int {
            return kotlin.math.max(0, bottom - top)
        }
    }


    private data class BadgeRow (val nextY: kotlin.Int, val hovered: dev.overgrown.origins.badge.Badge?, val powerId: net.minecraft.resources.ResourceLocation?) {
    }

}
