package andromeda.origins.client

import andromeda.origins.compat.VeilStepManager
import andromeda.origins.platform.ClientPlatform
import andromeda.origins.compat.VeilbornStealthCompat


 /** Keeps Veil Step flight responsive locally and tears it down immediately on emergence.  */
object VeilStepClient {
    var wasActive: kotlin.Boolean = false

    fun register() {
        ClientPlatform.startTick(java.util.function.Consumer callback@ { client: net.minecraft.client.Minecraft -> val player = client.player
        if (player == null) {
            VeilStepClient.wasActive = false
            return@callback
        }

        val active: kotlin.Boolean = VeilbornStealthCompat.isVeilStep(player)
        if (active) {
            val abilities: net.minecraft.world.entity.player.Abilities = player.getAbilities()
            abilities.mayfly = true
            abilities.flying = true
            abilities.setFlyingSpeed(VeilStepManager.VEIL_FLY_SPEED)
            player.setSprinting(false)
            player.resetFallDistance()
        } else if (VeilStepClient.wasActive) {
                // Stop residual traversal momentum immediately. The server re-sends the exact
                // pre-Veil mayfly/flying/speed state for a short restore window, which prevents
                // stale double-tap flight without clobbering legitimate Creative/modded flight.
            player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO)
            player.resetFallDistance()
        }

        VeilStepClient.wasActive = active

        })
    }
}
