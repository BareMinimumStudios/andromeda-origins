package andromeda.origins.client

import andromeda.origins.compat.FaerieJumpPress
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation

object FaerieJumpInput {
    private var state = FaerieJumpPress()
    private var player: net.minecraft.client.player.LocalPlayer? = null

    fun beginTick(client: Minecraft) {
        if (player !== client.player) {
            player = client.player
            state = FaerieJumpPress()
        }
        val current = client.player ?: return
        state.sample(client.options.keyJump.isDown, current.onGround())
    }

    fun blocks(power: ResourceLocation): Boolean = state.blocks(power)
}
