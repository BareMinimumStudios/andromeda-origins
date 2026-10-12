package andromeda.origins

import andromeda.origins.action.AndromedaActions
import andromeda.origins.compat.EnhancedFxCompat
import andromeda.origins.compat.VeilStepManager
import andromeda.origins.compat.MortalResolveManager
import andromeda.origins.config.AndromedaConfigBootstrap
import andromeda.origins.network.AndromedaConfigOpenNetworking
import andromeda.origins.config.OriginConfigCatalog
import andromeda.origins.AndromedaSounds
import andromeda.origins.compat.VersionHandshake
import andromeda.origins.command.AndromedaOriginsCommands
import andromeda.origins.compat.OriginsWorldConfigWarning
import andromeda.origins.compat.VeilbornShimmer
import andromeda.origins.compat.FiguraCompatManager


object AndromedaOrigins {
    const val MOD_ID: kotlin.String = "andromeda_origins"

     // Datapack, asset, registry, power, tag, and Figura-facing content uses the mod namespace.
    val CONTENT_NAMESPACE: kotlin.String = AndromedaOrigins.MOD_ID

    private fun registerIcon(path: kotlin.String) {
        andromeda.origins.platform.Platform.registerItem(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(CONTENT_NAMESPACE, path))
    }

    fun initialize() {
        AndromedaConfigBootstrap.initialize()
        AndromedaConfigOpenNetworking.registerServer()

        for (entry in OriginConfigCatalog.STANDARD) {
            AndromedaOrigins.registerIcon(entry.id.getPath() + "_icon")
        }

        AndromedaSounds.register()
        VersionHandshake.registerServer()
        EnhancedFxCompat.initialize()
        andromeda.origins.action.AndromedaConditions.register()
        AndromedaActions.register()
        AndromedaOriginsCommands.register()
        MortalResolveManager.register()
        OriginsWorldConfigWarning.register()
        VeilStepManager.register()
        VeilbornShimmer.register()

                // Clean Overgrown runtime: no automatic legacy-player migration or repair layer.
        FiguraCompatManager.register()
        andromeda.origins.compat.FoodBalanceCompat.register()

    }
}
