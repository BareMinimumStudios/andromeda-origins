package andromeda.origins

import andromeda.origins.AndromedaSounds
import andromeda.origins.action.AndromedaActions
import andromeda.origins.action.AndromedaConditions
import andromeda.origins.command.AndromedaOriginsCommands
import andromeda.origins.compat.CrowdControlLifecycle
import andromeda.origins.compat.Dawnbreaker
import andromeda.origins.compat.EnhancedFxCompat
import andromeda.origins.compat.FiguraCompatManager
import andromeda.origins.compat.FoodBalanceCompat
import andromeda.origins.compat.LichlingSunlight
import andromeda.origins.compat.MortalResolveManager
import andromeda.origins.compat.OriginsWorldConfigWarning
import andromeda.origins.compat.PlayerOriginRepair
import andromeda.origins.compat.RestraintCleanup
import andromeda.origins.compat.VeilStepManager
import andromeda.origins.compat.VeilbornShimmer
import andromeda.origins.compat.VersionHandshake
import andromeda.origins.compat.WebbedLifecycle
import andromeda.origins.config.AndromedaConfigBootstrap
import andromeda.origins.config.OriginConfigCatalog
import andromeda.origins.network.AndromedaConfigOpenNetworking
import andromeda.origins.platform.Platform
import net.minecraft.resources.ResourceLocation


object AndromedaOrigins {
    const val MOD_ID: String = "andromeda_origins"

     // Datapack, asset, registry, power, tag, and Figura-facing content uses the mod namespace.
    val CONTENT_NAMESPACE: String = AndromedaOrigins.MOD_ID

    private fun registerIcon(path: String) {
        Platform.registerItem(ResourceLocation.fromNamespaceAndPath(CONTENT_NAMESPACE, path))
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
        AndromedaConditions.register()
        AndromedaActions.register()
        AndromedaOriginsCommands.register()
        Dawnbreaker.register()
        MortalResolveManager.register()
        OriginsWorldConfigWarning.register()
        VeilStepManager.register()
        VeilbornShimmer.register()

        PlayerOriginRepair.register()
        FiguraCompatManager.register()
        CrowdControlLifecycle.register()
        WebbedLifecycle.register()
        RestraintCleanup.register()
        LichlingSunlight.register()
        FoodBalanceCompat.register()

    }
}
