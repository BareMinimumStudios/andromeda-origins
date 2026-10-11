package andromeda.origins.config

import andromeda.origins.config.AndromedaConfigRuntime
import andromeda.origins.config.AndromedaConfigPolicy


 /** Applies a config snapshot without destroying player power resources/cooldowns.  */
object AndromedaConfigApplier {
    fun applyToPlayer(player: net.minecraft.server.level.ServerPlayer) {
        AndromedaConfigApplier.reconcileSelectedOrigin(player)
        AndromedaConfigApplier.applyPowerSuppression(player)
    }

    fun applyToServer(server: net.minecraft.server.MinecraftServer) {
        for (player in server.getPlayerList().getPlayers()) AndromedaConfigApplier.applyToPlayer(player)
    }

    fun applyPowerSuppression(player: net.minecraft.server.level.ServerPlayer) {
        val container: dev.overgrown.apoli.power.PowerContainer? = dev.overgrown.apoli.power.PowerContainer.of(player)
        if (container == null) return
        for (power in java.util.List.copyOf<net.minecraft.resources.ResourceLocation>(container.allPowers())) {
            if (AndromedaConfigRuntime.shouldSuppressPower(player, power)) {
                container.suppressPower(power, AndromedaConfigPolicy.CONFIG_SUPPRESSION_SOURCE)
            } else {
                container.unsuppressPower(power, AndromedaConfigPolicy.CONFIG_SUPPRESSION_SOURCE)
            }
        }
    }

    private fun reconcileSelectedOrigin(player: net.minecraft.server.level.ServerPlayer) {
        val state: dev.overgrown.origins.component.PlayerOriginsImpl? = dev.overgrown.origins.component.PlayerOriginsAttachment.get(player)
        if (state == null) return
        val held: net.minecraft.resources.ResourceLocation? = state.getOrigin(AndromedaConfigPolicy.ORIGIN_LAYER)
        if (held == null || AndromedaConfigRuntime.originEnabled(held)) return

        dev.overgrown.origins.origin.OriginManager.removeOrigin(player, AndromedaConfigPolicy.ORIGIN_LAYER)
        val refreshed: dev.overgrown.origins.component.PlayerOriginsImpl = dev.overgrown.origins.component.PlayerOriginsAttachment.getOrCreate(player)
        val layer: dev.overgrown.origins.origin.OriginLayer? = dev.overgrown.origins.origin.OriginLayers.get(AndromedaConfigPolicy.ORIGIN_LAYER)
        if (layer != null) {
            refreshed.setSelectingOrigin(true)
            dev.overgrown.origins.network.OriginsServerNetwork.openChooseScreen(player, layer, false)
        }
        val server: net.minecraft.server.MinecraftServer? = player.getServer()
        if (server != null) dev.overgrown.origins.network.OriginsServerNetwork.broadcastPlayerOrigins(server, player)
    }
}
