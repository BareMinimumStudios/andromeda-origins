package andromeda.origins.network

import andromeda.origins.platform.Platform
import net.minecraft.server.level.ServerPlayer



object AndromedaConfigOpenNetworking {
    fun registerServer() {
        Platform.registerNetworking()}
    fun open(player: ServerPlayer) {
        Platform.openConfig(player)}
}
