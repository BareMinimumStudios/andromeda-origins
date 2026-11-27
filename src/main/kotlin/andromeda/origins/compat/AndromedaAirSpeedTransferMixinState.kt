package andromeda.origins.compat

import java.util.Set as JavaSet
import net.minecraft.resources.ResourceLocation

internal object AndromedaAirSpeedTransferMixinState {

        val `ANDROMEDA$TRANSFER_OWNERS`: MutableSet<ResourceLocation> = JavaSet.of<ResourceLocation>(
        AndromedaAirSpeedTransferMixinState.id("arachne/passives"),
        AndromedaAirSpeedTransferMixinState.id("champion/arachne_passives"),
        AndromedaAirSpeedTransferMixinState.id("faerie/passives"),
        AndromedaAirSpeedTransferMixinState.id("champion/faerie_passives"),
        AndromedaAirSpeedTransferMixinState.id("gorgon/helper/speedup"),
        AndromedaAirSpeedTransferMixinState.id("dawnbreaker/helper/transference_boost")
        )

        fun id(path: String): ResourceLocation {
            return ResourceLocation.fromNamespaceAndPath("andromeda_origins", path)
        }

}

