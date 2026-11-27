package andromeda.origins.compat

import dev.overgrown.apoli.power.PowerContainer
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

internal object NereidSubmersionDrowningMixinState {

        const val `ANDROMEDA$TAG`: String = "andromeda_nereid_submersion_drown"

        val `ANDROMEDA$DEBUFF`: ResourceLocation = ResourceLocation.fromNamespaceAndPath("andromeda_origins", "nereid/helper/whirlpooldebuff")

        val `APOLI$WATER_BREATHING_TYPE`: ResourceLocation = ResourceLocation.fromNamespaceAndPath("apoli", "water_breathing")
        fun `andromeda$shouldDrown`(player: ServerPlayer): Boolean {
            val container: PowerContainer? = PowerContainer.of(player)
            if (container == null || !container.hasPower(NereidSubmersionDrowningMixinState.`ANDROMEDA$DEBUFF`)) return false
            if (AndromedaOriginTags.hasChosenTag(player, AndromedaOriginTags.AQUATIC)) return false
            return container.powersOfType(NereidSubmersionDrowningMixinState.`APOLI$WATER_BREATHING_TYPE`).isEmpty()
        }

}

