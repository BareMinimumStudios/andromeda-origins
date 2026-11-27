package andromeda.origins.config

import java.util.Map as JavaMap
import java.util.Set as JavaSet
import net.minecraft.resources.ResourceLocation

/** Immutable server-authoritative gameplay configuration snapshot. */
class AndromedaConfigState(
    val enableStockOvergrownOrigins: Boolean,
    disabledStockOrigins: Set<ResourceLocation>,
    disabledAndromedaOrigins: Set<ResourceLocation>,
    disabledPowersByOrigin: Map<ResourceLocation, Set<ResourceLocation>>,
    val blockIncapacitatedFoodBypass: Boolean = true,
    val foodBalance: FoodBalanceState = FoodBalanceState(),
    val logoutDiagnostics: Boolean = false,
    val weaponBalance: WeaponBalanceState = WeaponBalanceState()
) {
    val disabledStockOrigins: Set<ResourceLocation> = JavaSet.copyOf(disabledStockOrigins)
    val disabledAndromedaOrigins: Set<ResourceLocation> = JavaSet.copyOf(disabledAndromedaOrigins)
    val disabledPowersByOrigin: Map<ResourceLocation, Set<ResourceLocation>> =
        JavaMap.copyOf(disabledPowersByOrigin.mapValues { JavaSet.copyOf(it.value) })

    fun powerDisabled(originId: ResourceLocation, powerId: ResourceLocation): Boolean =
        powerId in disabledPowersByOrigin[originId].orEmpty()

    companion object { val DEFAULT = AndromedaConfigState(false, emptySet(), emptySet(), emptyMap()) }
}
