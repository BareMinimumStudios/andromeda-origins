package andromeda.origins.client




 /** Shared neutral flat theme for Andromeda's Origin chooser and self-view screens.  */
object OriginUiTheme {
    const val PANEL: kotlin.Int = -0xe7e7e8
    const val SIDEBAR: kotlin.Int = -0xe3e3e4
    const val BORDER: kotlin.Int = -0xcacacb
    const val DIVIDER: kotlin.Int = -0xcfcfd0
    const val BUTTON: kotlin.Int = -0xd5d5d6
    const val BUTTON_HOVER: kotlin.Int = -0xc8c8c9
    const val BUTTON_SELECTED: kotlin.Int = -0xb9b9ba
    const val BUTTON_DISABLED: kotlin.Int = -0xdfdfe0
    const val BUTTON_BORDER: kotlin.Int = -0xafafb0
    const val BUTTON_BORDER_SELECTED: kotlin.Int = -0x757576
    const val TEXT: kotlin.Int = -0xf0f10
    const val MUTED: kotlin.Int = -0x555556
    const val BODY: kotlin.Int = -0x2f2f30
    const val SCROLL_TRACK: kotlin.Int = -0xd9d9da
    const val SCROLL_THUMB: kotlin.Int = -0x888889

    fun panel(graphics: net.minecraft.client.gui.GuiGraphics, left: kotlin.Int, top: kotlin.Int, right: kotlin.Int, bottom: kotlin.Int) {
        graphics.fill(left, top, right, bottom, OriginUiTheme.PANEL)
        graphics.fill(left, top, right, top + 1, OriginUiTheme.BORDER)
        graphics.fill(left, bottom - 1, right, bottom, OriginUiTheme.BORDER)
        graphics.fill(left, top, left + 1, bottom, OriginUiTheme.BORDER)
        graphics.fill(right - 1, top, right, bottom, OriginUiTheme.BORDER)
    }

    fun flatButton(graphics: net.minecraft.client.gui.GuiGraphics, font: net.minecraft.client.gui.Font, button: net.minecraft.client.gui.components.Button?,
    mouseX: kotlin.Int, mouseY: kotlin.Int, selected: kotlin.Boolean) {
        if (button == null || !button.visible) return
        val x: kotlin.Int = button.getX()
        val y: kotlin.Int = button.getY()
        val width: kotlin.Int = button.getWidth()
        val height: kotlin.Int = button.getHeight()
        val hovered: kotlin.Boolean = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height

        val fill: kotlin.Int = if (!button.active)
        OriginUiTheme.BUTTON_DISABLED
        else
        if (selected)
        OriginUiTheme.BUTTON_SELECTED
        else
        if (hovered)
        OriginUiTheme.BUTTON_HOVER
        else
        OriginUiTheme.BUTTON
        val border: kotlin.Int = if (selected) OriginUiTheme.BUTTON_BORDER_SELECTED else OriginUiTheme.BUTTON_BORDER
        val text: kotlin.Int = if (button.active) OriginUiTheme.TEXT else -0x888889

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
