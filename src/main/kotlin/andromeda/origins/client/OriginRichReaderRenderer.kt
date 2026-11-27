package andromeda.origins.client

import andromeda.origins.client.OriginUiTheme
import andromeda.origins.client.RecipeBadgeTooltip
import com.mojang.blaze3d.systems.RenderSystem
import dev.overgrown.apoli.client.IconRenderer
import dev.overgrown.apoli.power.ApoliPowers
import dev.overgrown.apoli.power.Power
import dev.overgrown.origins.badge.Badge
import dev.overgrown.origins.badge.CraftingRecipeBadge
import dev.overgrown.origins.client.BadgeClientState
import dev.overgrown.origins.origin.Origin
import kotlin.math.max
import kotlin.math.min
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player


 /**
 * Shared rich-text Origin document renderer used by both the chooser and the O-key self viewer.
 *
 *
 * This class deliberately owns only presentation. Origin state, power visibility, selection,
 * networking, scrolling input and layer flow remain owned by Overgrown's screens/mixins.
 */
object OriginRichReaderRenderer {
    private const val LINE_HEIGHT: Int = 11

     /** Renders the current Origin document and returns the maximum valid scroll offset.  */
    fun render(graphics: GuiGraphics, font: Font, origin: Origin?, viewer: Player?,
    bounds: OriginRichReaderRenderer.Bounds, scrollPos: Int, mouseX: Int, mouseY: Int, partialTick: Float): Int {
        if (origin == null || bounds.width() <= 0 || bounds.height() <= 0) return 0

        val left: Int = bounds.left
        val top: Int = bounds.top
        val right: Int = bounds.right
        val bottom: Int = bounds.bottom

        val headerY: Int = top + 5
        IconRenderer.render(graphics, origin.icon(), left + 13, headerY + 1)
        val originName: MutableComponent = origin.name().copy().withStyle(ChatFormatting.BOLD)
        graphics.drawString(font, originName, left + 37, headerY + 1, OriginUiTheme.TEXT, false)
        val impactName: Component = Component.translatable("origins.gui.impact." + origin.impact().name.lowercase())
        val impact: Component = Component.translatable("andromeda_origins.screen.reader.impact", impactName).withStyle(ChatFormatting.GRAY)
        graphics.drawString(font, impact,
        max(left + 37, right - 14 - font.width(impact)),
        headerY + 1, OriginUiTheme.MUTED, false)
        graphics.fill(left + 12, headerY + 20, right - 12, headerY + 21, OriginUiTheme.DIVIDER)

        val contentLeft: Int = left + 15
        val contentRight: Int = right - 16
        val contentTop: Int = headerY + 29
        val contentBottom: Int = bottom - 8
        val textWidth: Int = max(90, contentRight - contentLeft - 8)
        var y: Int = contentTop - scrollPos
        var hoveredBadge: Badge? = null
        var hoveredBadgePowerId: ResourceLocation? = null

        graphics.enableScissor(contentLeft - 2, contentTop, contentRight + 2, contentBottom)

        y = OriginRichReaderRenderer.drawSection(graphics, font, Component.translatable("andromeda_origins.screen.reader.overview"), y, contentLeft, textWidth)
        y = OriginRichReaderRenderer.drawWrapped(graphics, font, origin.description(), y, contentLeft + 2,
        textWidth - 4, OriginUiTheme.BODY, OriginRichReaderRenderer.LINE_HEIGHT)
        y += 10

        y = OriginRichReaderRenderer.drawSection(graphics, font, Component.translatable("andromeda_origins.screen.reader.powers_traits"), y, contentLeft, textWidth)
        val shownPowers: MutableList<ResourceLocation> = if (viewer != null) origin.powersFor(viewer) else origin.powers()
        for (powerId in shownPowers) {
            val power: Power? = ApoliPowers.get(powerId)
            if (power == null || power.hidden()) continue

            val powerName: MutableComponent = Component.literal("• ")
            .withStyle(ChatFormatting.GRAY)
            .append(power.displayName(powerId).copy().withStyle(ChatFormatting.BOLD, ChatFormatting.WHITE))
            y = OriginRichReaderRenderer.drawWrapped(graphics, font, powerName, y, contentLeft, textWidth,
            OriginUiTheme.TEXT, OriginRichReaderRenderer.LINE_HEIGHT)

            val badges: MutableList<Badge> = BadgeClientState.get(powerId)
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

        val fullHeight: Int = (y + scrollPos) - contentTop
        val viewport: Int = max(1, contentBottom - contentTop)
        val maxScroll: Int = max(0, fullHeight - viewport)
        OriginRichReaderRenderer.renderScrollbar(graphics, right, contentTop, contentBottom, scrollPos, maxScroll, fullHeight, viewport)

        if (hoveredBadge != null && hoveredBadge.hasTooltip()) {
            val widthLimit: Int = max(120, bounds.right - mouseX - 18)
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            if (hoveredBadge is CraftingRecipeBadge) {
                RecipeBadgeTooltip.render(graphics, font, hoveredBadge, mouseX, mouseY, widthLimit)
            } else {
                hoveredBadge.renderTooltip(graphics, font, mouseX, mouseY, widthLimit, hoveredBadgePowerId, partialTick)
            }
        }
        return maxScroll
    }

    private fun drawBadges(graphics: GuiGraphics, badges: MutableList<Badge>, powerId: ResourceLocation,
    startX: Int, right: Int, y: Int, mouseX: Int, mouseY: Int,
    clipTop: Int, clipBottom: Int): BadgeRow {
        val size: Int = 9
        val step: Int = 11
        var x: Int = startX
        var rowY: Int = y
        var hovered: Badge? = null

        for (badge in badges) {
            if (x + size > right) {
                x = startX
                rowY += step
            }
            if (rowY >= clipTop - size && rowY <= clipBottom + size) {
                val sprite: ResourceLocation = OriginRichReaderRenderer.resolveBadgeSprite(badge)
                val spriteSize: Int = OriginRichReaderRenderer.badgeSpriteSize(sprite)
                RenderSystem.enableBlend()
                RenderSystem.defaultBlendFunc()
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
                graphics.blit(sprite, x, rowY, size, size,
                0.0f, 0.0f, spriteSize, spriteSize, spriteSize, spriteSize)
                if (badge.hasTooltip() && mouseX >= x && mouseX < x + size && mouseY >= max(rowY, clipTop) && mouseY < min(rowY + size, clipBottom)) {
                    hovered = badge
                }
            }
            x += step
        }
        return BadgeRow(rowY + step + 1, hovered, if (hovered == null) null else powerId)
    }

    private fun resolveBadgeSprite(badge: Badge): ResourceLocation {
        val minecraft: Minecraft = Minecraft.getInstance()
        val requested = badge.spriteId()
        if (minecraft.getResourceManager().getResource(requested).isPresent()) return requested
        for (fallback in badge.fallback()) {
            if (minecraft.getResourceManager().getResource(fallback).isPresent()) return fallback
        }

        // Native badge artwork stays in its dependency. Missing resources use our own
        // informational glyph while retaining the badge's full tooltip and behavior.
        return ResourceLocation.fromNamespaceAndPath(
            "andromeda_origins", "textures/gui/badge/fallback.png")
    }

    private fun badgeSpriteSize(sprite: ResourceLocation?): Int {
        // BadgeClientState caches dimensions by id, and works for both Origins' sprite namespace
        // and our independently drawn fallback glyph.
        return BadgeClientState.spriteSize(sprite)
    }

    private fun drawSection(graphics: GuiGraphics, font: Font, label: Component, y: Int, x: Int, width: Int): Int {
        val heading: MutableComponent = label.copy().withStyle(ChatFormatting.BOLD, ChatFormatting.WHITE)
        graphics.drawString(font, heading, x, y, -0x1f1f20, false)
        val lineX: Int = x + font.width(heading) + 7
        if (lineX < x + width) {
            graphics.fill(lineX, y + 5, x + width, y + 6, OriginUiTheme.DIVIDER)
        }
        return y + 15
    }

    private fun drawWrapped(graphics: GuiGraphics, font: Font, component: Component, y: Int, x: Int,
    width: Int, fallbackColor: Int, lineHeight: Int): Int {
        var y: Int = y
        for (line in font.split(component, max(40, width))) {
            graphics.drawString(font, line, x, y, fallbackColor, false)
            y += lineHeight
        }
        return y
    }

    private fun renderScrollbar(graphics: GuiGraphics, right: Int, trackTop: Int, trackBottom: Int,
    scrollPos: Int, maxScroll: Int, fullHeight: Int, viewport: Int) {
        if (maxScroll <= 0) return
        val trackX: Int = right - 8
        graphics.fill(trackX, trackTop, trackX + 3, trackBottom, OriginUiTheme.SCROLL_TRACK)
        val trackHeight: Int = trackBottom - trackTop
        val thumbHeight: Int = max(18,
        ((viewport / max(viewport, fullHeight).toFloat()) * trackHeight).toInt())
        val travel: Int = max(1, trackHeight - thumbHeight)
        val thumbY: Int = trackTop + ((scrollPos / maxScroll.toFloat()) * travel).toInt()
        graphics.fill(trackX - 1, thumbY, trackX + 4, thumbY + thumbHeight, OriginUiTheme.SCROLL_THUMB)
    }

    data class Bounds (val left: Int, val top: Int, val right: Int, val bottom: Int) {
        fun width(): Int {
            return max(0, right - left)
        }

        fun height(): Int {
            return max(0, bottom - top)
        }
    }


    private data class BadgeRow (val nextY: Int, val hovered: Badge?, val powerId: ResourceLocation?) {
    }

}
