package andromeda.origins.network



object AndromedaConfigOpenNetworking {
    fun registerServer() {
        andromeda.origins.platform.Platform.registerNetworking()}
    fun open(player: net.minecraft.server.level.ServerPlayer) {
        andromeda.origins.platform.Platform.openConfig(player)}
}
