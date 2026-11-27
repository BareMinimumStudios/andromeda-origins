package andromeda.origins.compat

import andromeda.origins.platform.Platform



object VersionHandshake {
    fun currentVersion(): String {
        return Platform.version()}
    fun registerServer() {
        Platform.versionHandshake()}
}
