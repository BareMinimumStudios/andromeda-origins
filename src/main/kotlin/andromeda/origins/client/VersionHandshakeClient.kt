package andromeda.origins.client

import andromeda.origins.platform.ClientPlatform



object VersionHandshakeClient {
    fun register() {
        ClientPlatform.versionHandshake()}
}
