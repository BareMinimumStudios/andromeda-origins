package andromeda.origins.compat

internal object AndromedaAirSpeedTransferMixinState {

        val `ANDROMEDA$TRANSFER_OWNERS`: kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation> = java.util.Set.of<net.minecraft.resources.ResourceLocation>(
        AndromedaAirSpeedTransferMixinState.id("arachne/passives"),
        AndromedaAirSpeedTransferMixinState.id("champion/arachne_passives"),
        AndromedaAirSpeedTransferMixinState.id("faerie/passives"),
        AndromedaAirSpeedTransferMixinState.id("champion/faerie_passives"),
        AndromedaAirSpeedTransferMixinState.id("gorgon/helper/speedup")
        )

        fun id(path: kotlin.String): net.minecraft.resources.ResourceLocation {
            return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("andromeda_origins", path)
        }

}

