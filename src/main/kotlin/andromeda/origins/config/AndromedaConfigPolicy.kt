package andromeda.origins.config

import andromeda.origins.AndromedaOrigins
import andromeda.origins.config.AndromedaConfigState
import andromeda.origins.config.OriginConfigCatalog
import dev.overgrown.origins.component.PlayerOriginsAttachment
import dev.overgrown.origins.component.PlayerOriginsImpl
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.Map as JavaMap
import java.util.Set as JavaSet
import java.util.stream.Collectors
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player


 /** IDs and grouping rules for safe, player-facing configuration.  */
object AndromedaConfigPolicy {
    val ORIGIN_LAYER: ResourceLocation = ResourceLocation.fromNamespaceAndPath("origins", "origin")
    val CONFIG_SUPPRESSION_SOURCE: ResourceLocation = ResourceLocation.fromNamespaceAndPath(AndromedaOrigins.MOD_ID, "config_disabled")

    val STANDARD_ORIGINS: MutableList<ResourceLocation> = OriginConfigCatalog.standardIds()
    val CHAMPION_ORIGINS: MutableList<ResourceLocation> = OriginConfigCatalog.championIds()
    val STOCK_ORIGINS: MutableList<ResourceLocation> = OriginConfigCatalog.stockIds()

    val IRON_WEAKNESS: ResourceLocation = AndromedaConfigPolicy.andromeda("common/ironweakness")
    val IRON_WEAKNESS_HELPERS: MutableSet<ResourceLocation> = JavaSet.of<ResourceLocation>(
    AndromedaConfigPolicy.andromeda("common/witheringironweakness"), AndromedaConfigPolicy.andromeda("common/witheringironhit")
    )

    fun isAndromedaOrigin(id: ResourceLocation?): Boolean {
        return OriginConfigCatalog.isAndromedaOrigin(id)
    }

    fun isStockOrigin(id: ResourceLocation?): Boolean {
        return OriginConfigCatalog.isStockOrigin(id)
    }

    fun isOriginEnabled(id: ResourceLocation, state: AndromedaConfigState): Boolean {
        if (AndromedaConfigPolicy.isAndromedaOrigin(id)) return !state.disabledAndromedaOrigins.contains(id)
        if (AndromedaConfigPolicy.isStockOrigin(id)) {
            return state.enableStockOvergrownOrigins && !state.disabledStockOrigins.contains(id)
        }
        return true // Do not interfere with third-party Origin addons/layers.
    }

    fun filterPrimaryLayer(input: MutableList<ResourceLocation>, state: AndromedaConfigState): MutableList<ResourceLocation> {
        if (input.isEmpty()) return input
        return input.stream().filter { id: ResourceLocation -> AndromedaConfigPolicy.isOriginEnabled(id, state)} .toList()
    }

     /** Maps implementation companions back to their single admin-facing weakness control.  */
    fun controlPower(power: ResourceLocation): ResourceLocation {
        if (AndromedaConfigPolicy.IRON_WEAKNESS_HELPERS.contains(power)) return AndromedaConfigPolicy.IRON_WEAKNESS
        return power
    }

    fun configurablePowers(originId: ResourceLocation?): MutableSet<ResourceLocation> {
        return OriginConfigCatalog.configurablePowers(originId)
    }

    fun isConfigurablePower(originId: ResourceLocation, power: ResourceLocation): Boolean {
        val control: ResourceLocation = AndromedaConfigPolicy.controlPower(power)
        if (OriginConfigCatalog.NON_CONFIGURABLE_INFRASTRUCTURE.contains(control)) return false
        return AndromedaConfigPolicy.configurablePowers(originId).contains(control)
    }

    fun shouldSuppressPower(owner: Entity, power: ResourceLocation, state: AndromedaConfigState): Boolean {
        val player = owner as? Player
        if (player == null) return false
        if (AndromedaOrigins.MOD_ID != power.getNamespace()) return false
        val origins: PlayerOriginsImpl? = PlayerOriginsAttachment.get(owner)
        if (origins == null) return false
        val originId: ResourceLocation? = origins.getOrigin(AndromedaConfigPolicy.ORIGIN_LAYER)
        if (originId == null || !AndromedaConfigPolicy.isAndromedaOrigin(originId)) return false
        val control: ResourceLocation = AndromedaConfigPolicy.controlPower(power)
        return AndromedaConfigPolicy.isConfigurablePower(originId, control) && state.powerDisabled(originId, control)
    }

    fun sanitizeAndromedaOrigins(values: MutableSet<ResourceLocation>): MutableSet<ResourceLocation> {
        return values.stream().filter { obj: ResourceLocation -> OriginConfigCatalog.isAndromedaOrigin(obj)}
        .collect(Collectors.toUnmodifiableSet())
    }

    fun sanitizeStockOrigins(values: MutableSet<ResourceLocation>): MutableSet<ResourceLocation> {
        return values.stream().filter { obj: ResourceLocation -> OriginConfigCatalog.isStockOrigin(obj)}
        .collect(Collectors.toUnmodifiableSet())
    }

    fun sanitizePowerMap(
    values: MutableMap<ResourceLocation, MutableSet<ResourceLocation>>?): MutableMap<ResourceLocation, MutableSet<ResourceLocation>> {
        if (values == null || values.isEmpty()) return JavaMap.of<ResourceLocation, MutableSet<ResourceLocation>>()
        val out: MutableMap<ResourceLocation, MutableSet<ResourceLocation>> = LinkedHashMap<ResourceLocation, MutableSet<ResourceLocation>>()
        values.forEach { (origin: ResourceLocation, powers: MutableSet<ResourceLocation>) -> if (!OriginConfigCatalog.isAndromedaOrigin(origin)) return@forEach
        val clean: MutableSet<ResourceLocation> = LinkedHashSet<ResourceLocation>()
        for (power in powers) {
            val control: ResourceLocation = AndromedaConfigPolicy.controlPower(power)
            if (AndromedaConfigPolicy.isConfigurablePower(origin, control)) clean.add(control)
        }
        if (!clean.isEmpty()) out.put(origin, JavaSet.copyOf<ResourceLocation>(clean))

        }
        return JavaMap.copyOf<ResourceLocation, MutableSet<ResourceLocation>>(out)
    }


    fun andromeda(path: String): ResourceLocation {
        return OriginConfigCatalog.andromeda(path)
    }
}
