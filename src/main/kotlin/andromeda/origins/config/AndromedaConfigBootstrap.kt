package andromeda.origins.config

import andromeda.origins.platform.JoinCallback
import andromeda.origins.config.AndromedaConfigApplier
import andromeda.origins.config.AndromedaConfigRuntime
import andromeda.origins.config.AndromedaGameplayConfig


 /** Registers the Fzzy Config gameplay config and applies it after Origins restores player powers.  */
object AndromedaConfigBootstrap {
    private lateinit var config: AndromedaGameplayConfig

    fun initialize() {
        AndromedaConfigBootstrap.config = me.fzzyhmstrs.fzzy_config.api.ConfigApiJava.registerAndLoadConfig<AndromedaGameplayConfig>(java.util.function.Supplier {AndromedaGameplayConfig()}, me.fzzyhmstrs.fzzy_config.api.RegisterType.BOTH)
        AndromedaConfigRuntime.set(AndromedaConfigBootstrap.config.toState())

                // Origins is a hard dependency and registers its JOIN callback first. Run after its
        // reapplyAll() so persisted power ownership exists before config suppression is reconciled.
        andromeda.origins.platform.Platform.join(JoinCallback callback@ { handler: net.minecraft.server.network.ServerGamePacketListenerImpl, sender: kotlin.Any?, server: net.minecraft.server.MinecraftServer -> AndromedaConfigApplier.applyToPlayer(handler.player)})
    }

    fun config(): AndromedaGameplayConfig {
        kotlin.checkNotNull(AndromedaConfigBootstrap.config) {"Andromeda gameplay config has not been initialized"}
        return AndromedaConfigBootstrap.config
    }
}
