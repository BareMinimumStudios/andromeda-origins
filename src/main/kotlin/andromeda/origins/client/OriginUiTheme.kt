package andromeda.origins.client

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button




 /** Shared neutral flat theme for Andromeda's Origin chooser and self-view screens.  */
object OriginUiTheme {
    const val PANEL: Int = -0xe7e7e8
    const val SIDEBAR: Int = -0xe3e3e4
    const val BORDER: Int = -0xcacacb
    const val DIVIDER: Int = -0xcfcfd0
    const val BUTTON: Int = -0xd5d5d6
    const val BUTTON_HOVER: Int = -0xc8c8c9
    const val BUTTON_SELECTED: Int = -0xb9b9ba
    const val BUTTON_DISABLED: Int = -0xdfdfe0
    const val BUTTON_BORDER: Int = -0xafafb0
    const val BUTTON_BORDER_SELECTED: Int = -0x757576
    const val TEXT: Int = -0xf0f10
    const val MUTED: Int = -0x555556
    const val BODY: Int = -0x2f2f30
    const val SCROLL_TRACK: Int = -0xd9d9da
    const val SCROLL_THUMB: Int = -0x888889

    fun panel(graphics: GuiGraphics, left: Int, top: Int, right: Int, bottom: Int) {
        graphics.fill(left, top, right, bottom, OriginUiTheme.PANEL)
        graphics.fill(left, top, right, top + 1, OriginUiTheme.BORDER)
        graphics.fill(left, bottom - 1, right, bottom, OriginUiTheme.BORDER)
        graphics.fill(left, top, left + 1, bottom, OriginUiTheme.BORDER)
        graphics.fill(right - 1, top, right, bottom, OriginUiTheme.BORDER)
    }

    fun flatButton(graphics: GuiGraphics, font: Font, button: Button?,
    mouseX: Int, mouseY: Int, selected: Boolean) {
        if (button == null || !button.visible) return
        val x: Int = button.getX()
        val y: Int = button.getY()
        val width: Int = button.getWidth()
        val height: Int = button.getHeight()
        val hovered: Boolean = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height

        val fill: Int = if (!button.active)
        OriginUiTheme.BUTTON_DISABLED
        else
        if (selected)
        OriginUiTheme.BUTTON_SELECTED
        else
        if (hovered)
        OriginUiTheme.BUTTON_HOVER
        else
        OriginUiTheme.BUTTON
        val border: Int = if (selected) OriginUiTheme.BUTTON_BORDER_SELECTED else OriginUiTheme.BUTTON_BORDER
        val text: Int = if (button.active) OriginUiTheme.TEXT else -0x888889

        graphics.fill(x, y, x + width, y + height, fill)
        graphics.fill(x, y, x + width, y + 1, border)
        graphics.fill(x, y + height - 1, x + width, y + height, border)
        graphics.fill(x, y, x + 1, y + height, border)
        graphics.fill(x + width - 1, y, x + width, y + height, border)

        if (!button.getMessage().getString().isEmpty()) {
            graphics.drawCenteredString(font, button.getMessage(),
            x + width / 2, y + (height - 8) / 2, text)
        }
    }
}
