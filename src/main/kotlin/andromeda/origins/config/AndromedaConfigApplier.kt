package andromeda.origins.config

import andromeda.origins.config.AndromedaConfigPolicy
import andromeda.origins.config.AndromedaConfigRuntime
import dev.overgrown.apoli.power.PowerContainer
import dev.overgrown.origins.component.PlayerOriginsAttachment
import dev.overgrown.origins.component.PlayerOriginsImpl
import dev.overgrown.origins.network.OriginsServerNetwork
import dev.overgrown.origins.origin.OriginLayer
import dev.overgrown.origins.origin.OriginLayers
import dev.overgrown.origins.origin.OriginManager
import java.util.List as JavaList
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer


 /** Applies a config snapshot without destroying player power resources/cooldowns.  */
object AndromedaConfigApplier {
    fun applyToPlayer(player: ServerPlayer) {
        AndromedaConfigApplier.reconcileSelectedOrigin(player)
        AndromedaConfigApplier.applyPowerSuppression(player)
    }

    fun applyToServer(server: MinecraftServer) {
        for (player in server.getPlayerList().getPlayers()) AndromedaConfigApplier.applyToPlayer(player)
    }

    fun applyPowerSuppression(player: ServerPlayer) {
        val container: PowerContainer? = PowerContainer.of(player)
        if (container == null) return
        for (power in JavaList.copyOf<ResourceLocation>(container.allPowers())) {
            if (AndromedaConfigRuntime.shouldSuppressPower(player, power)) {
                container.suppressPower(power, AndromedaConfigPolicy.CONFIG_SUPPRESSION_SOURCE)
            } else {
                container.unsuppressPower(power, AndromedaConfigPolicy.CONFIG_SUPPRESSION_SOURCE)
            }
        }
    }

    private fun reconcileSelectedOrigin(player: ServerPlayer) {
        val state: PlayerOriginsImpl? = PlayerOriginsAttachment.get(player)
        if (state == null) return
        val held: ResourceLocation? = state.getOrigin(AndromedaConfigPolicy.ORIGIN_LAYER)
        if (held == null || AndromedaConfigRuntime.originEnabled(held)) return

        OriginManager.removeOrigin(player, AndromedaConfigPolicy.ORIGIN_LAYER)
        val refreshed: PlayerOriginsImpl = PlayerOriginsAttachment.getOrCreate(player)
        val layer: OriginLayer? = OriginLayers.get(AndromedaConfigPolicy.ORIGIN_LAYER)
        if (layer != null) {
            refreshed.setSelectingOrigin(true)
            OriginsServerNetwork.openChooseScreen(player, layer, false)
        }
        val server: MinecraftServer? = player.getServer()
        if (server != null) OriginsServerNetwork.broadcastPlayerOrigins(server, player)
    }
}
