package andromeda.origins.compat

internal object NereidSubmersionDrowningMixinState {

        const val `ANDROMEDA$TAG`: kotlin.String = "andromeda_nereid_submersion_drown"

        val `ANDROMEDA$DEBUFF`: net.minecraft.resources.ResourceLocation = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("andromeda_origins", "nereid/helper/whirlpooldebuff")

        val `APOLI$WATER_BREATHING_TYPE`: net.minecraft.resources.ResourceLocation = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("apoli", "water_breathing")
        fun `andromeda$shouldDrown`(player: net.minecraft.server.level.ServerPlayer): kotlin.Boolean {
            val container: dev.overgrown.apoli.power.PowerContainer? = dev.overgrown.apoli.power.PowerContainer.of(player)
            if (container == null || !container.hasPower(NereidSubmersionDrowningMixinState.`ANDROMEDA$DEBUFF`)) return false
            if (AndromedaOriginTags.hasChosenTag(player, AndromedaOriginTags.AQUATIC)) return false
            return container.powersOfType(NereidSubmersionDrowningMixinState.`APOLI$WATER_BREATHING_TYPE`).isEmpty()
        }

}

