package andromeda.origins.config

import andromeda.origins.config.AndromedaConfigApplier
import andromeda.origins.config.AndromedaConfigRuntime
import andromeda.origins.config.AndromedaGameplayConfig
import andromeda.origins.platform.JoinCallback
import andromeda.origins.platform.Platform
import java.util.function.Supplier
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava
import me.fzzyhmstrs.fzzy_config.api.RegisterType
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerGamePacketListenerImpl


 /** Registers the Fzzy Config gameplay config and applies it after Origins restores player powers.  */
object AndromedaConfigBootstrap {
    private lateinit var config: AndromedaGameplayConfig

    fun initialize() {
        AndromedaConfigBootstrap.config = ConfigApiJava.registerAndLoadConfig<AndromedaGameplayConfig>(Supplier {AndromedaGameplayConfig()}, RegisterType.BOTH)
        AndromedaConfigRuntime.set(AndromedaConfigBootstrap.config.toState())

                // Origins is a hard dependency and registers its JOIN callback first. Run after its
        // reapplyAll() so persisted power ownership exists before config suppression is reconciled.
        Platform.join(JoinCallback callback@ { handler: ServerGamePacketListenerImpl, sender: Any?, server: MinecraftServer -> AndromedaConfigApplier.applyToPlayer(handler.player)})
    }

    fun config(): AndromedaGameplayConfig {
        checkNotNull(AndromedaConfigBootstrap.config) {"Andromeda gameplay config has not been initialized"}
        return AndromedaConfigBootstrap.config
    }
}
