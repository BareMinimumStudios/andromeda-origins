package andromeda.origins.compat



object VersionHandshake {
    fun currentVersion(): kotlin.String {
        return andromeda.origins.platform.Platform.version()}
    fun registerServer() {
        andromeda.origins.platform.Platform.versionHandshake()}
}
