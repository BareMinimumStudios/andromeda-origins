package andromeda.origins.client



object VersionHandshakeClient {
    fun register() {
        andromeda.origins.platform.ClientPlatform.versionHandshake()}
}
