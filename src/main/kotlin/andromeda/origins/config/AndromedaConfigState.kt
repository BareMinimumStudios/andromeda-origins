package andromeda.origins.config

import net.minecraft.resources.ResourceLocation

/** Immutable server-authoritative gameplay configuration snapshot. */
class AndromedaConfigState(
    val enableStockOvergrownOrigins: Boolean,
    disabledStockOrigins: Set<ResourceLocation>,
    disabledAndromedaOrigins: Set<ResourceLocation>,
    disabledPowersByOrigin: Map<ResourceLocation, Set<ResourceLocation>>,
    val blockIncapacitatedFoodBypass: Boolean = true,
    val foodBalance: FoodBalanceState = FoodBalanceState()
) {
    val disabledStockOrigins: Set<ResourceLocation> = java.util.Set.copyOf(disabledStockOrigins)
    val disabledAndromedaOrigins: Set<ResourceLocation> = java.util.Set.copyOf(disabledAndromedaOrigins)
    val disabledPowersByOrigin: Map<ResourceLocation, Set<ResourceLocation>> =
        java.util.Map.copyOf(disabledPowersByOrigin.mapValues { java.util.Set.copyOf(it.value) })

    fun powerDisabled(originId: ResourceLocation, powerId: ResourceLocation): Boolean =
        powerId in disabledPowersByOrigin[originId].orEmpty()

    companion object { val DEFAULT = AndromedaConfigState(false, emptySet(), emptySet(), emptyMap()) }
}
