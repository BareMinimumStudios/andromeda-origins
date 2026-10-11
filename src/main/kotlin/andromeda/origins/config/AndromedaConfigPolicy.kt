package andromeda.origins.config

import andromeda.origins.AndromedaOrigins
import andromeda.origins.config.OriginConfigCatalog
import andromeda.origins.config.AndromedaConfigState
import java.util.LinkedHashMap
import java.util.LinkedHashSet


 /** IDs and grouping rules for safe, player-facing configuration.  */
object AndromedaConfigPolicy {
    val ORIGIN_LAYER: net.minecraft.resources.ResourceLocation = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("origins", "origin")
    val CONFIG_SUPPRESSION_SOURCE: net.minecraft.resources.ResourceLocation = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(AndromedaOrigins.MOD_ID, "config_disabled")

    val STANDARD_ORIGINS: kotlin.collections.MutableList<net.minecraft.resources.ResourceLocation> = OriginConfigCatalog.standardIds()
    val CHAMPION_ORIGINS: kotlin.collections.MutableList<net.minecraft.resources.ResourceLocation> = OriginConfigCatalog.championIds()
    val STOCK_ORIGINS: kotlin.collections.MutableList<net.minecraft.resources.ResourceLocation> = OriginConfigCatalog.stockIds()

    val IRON_WEAKNESS: net.minecraft.resources.ResourceLocation = AndromedaConfigPolicy.andromeda("common/ironweakness")
    val IRON_WEAKNESS_HELPERS: kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation> = java.util.Set.of<net.minecraft.resources.ResourceLocation>(
    AndromedaConfigPolicy.andromeda("common/witheringironweakness"), AndromedaConfigPolicy.andromeda("common/witheringironhit")
    )

    fun isAndromedaOrigin(id: net.minecraft.resources.ResourceLocation?): kotlin.Boolean {
        return OriginConfigCatalog.isAndromedaOrigin(id)
    }

    fun isStockOrigin(id: net.minecraft.resources.ResourceLocation?): kotlin.Boolean {
        return OriginConfigCatalog.isStockOrigin(id)
    }

    fun isOriginEnabled(id: net.minecraft.resources.ResourceLocation, state: AndromedaConfigState): kotlin.Boolean {
        if (AndromedaConfigPolicy.isAndromedaOrigin(id)) return !state.disabledAndromedaOrigins.contains(id)
        if (AndromedaConfigPolicy.isStockOrigin(id)) {
            return state.enableStockOvergrownOrigins && !state.disabledStockOrigins.contains(id)
        }
        return true // Do not interfere with third-party Origin addons/layers.
    }

    fun filterPrimaryLayer(input: kotlin.collections.MutableList<net.minecraft.resources.ResourceLocation>, state: AndromedaConfigState): kotlin.collections.MutableList<net.minecraft.resources.ResourceLocation> {
        if (input.isEmpty()) return input
        return input.stream().filter { id: net.minecraft.resources.ResourceLocation -> AndromedaConfigPolicy.isOriginEnabled(id, state)} .toList()
    }

     /** Maps implementation companions back to their single admin-facing weakness control.  */
    fun controlPower(power: net.minecraft.resources.ResourceLocation): net.minecraft.resources.ResourceLocation {
        if (AndromedaConfigPolicy.IRON_WEAKNESS_HELPERS.contains(power)) return AndromedaConfigPolicy.IRON_WEAKNESS
        return power
    }

    fun configurablePowers(originId: net.minecraft.resources.ResourceLocation?): kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation> {
        return OriginConfigCatalog.configurablePowers(originId)
    }

    fun isConfigurablePower(originId: net.minecraft.resources.ResourceLocation, power: net.minecraft.resources.ResourceLocation): kotlin.Boolean {
        val control: net.minecraft.resources.ResourceLocation = AndromedaConfigPolicy.controlPower(power)
        if (OriginConfigCatalog.NON_CONFIGURABLE_INFRASTRUCTURE.contains(control)) return false
        return AndromedaConfigPolicy.configurablePowers(originId).contains(control)
    }

    fun shouldSuppressPower(owner: net.minecraft.world.entity.Entity, power: net.minecraft.resources.ResourceLocation, state: AndromedaConfigState): kotlin.Boolean {
        val player = owner as? net.minecraft.world.entity.player.Player
        if (player == null) return false
        if (AndromedaOrigins.MOD_ID != power.getNamespace()) return false
        val origins: dev.overgrown.origins.component.PlayerOriginsImpl? = dev.overgrown.origins.component.PlayerOriginsAttachment.get(owner)
        if (origins == null) return false
        val originId: net.minecraft.resources.ResourceLocation? = origins.getOrigin(AndromedaConfigPolicy.ORIGIN_LAYER)
        if (originId == null || !AndromedaConfigPolicy.isAndromedaOrigin(originId)) return false
        val control: net.minecraft.resources.ResourceLocation = AndromedaConfigPolicy.controlPower(power)
        return AndromedaConfigPolicy.isConfigurablePower(originId, control) && state.powerDisabled(originId, control)
    }

    fun sanitizeAndromedaOrigins(values: kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation>): kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation> {
        return values.stream().filter { obj: net.minecraft.resources.ResourceLocation -> OriginConfigCatalog.isAndromedaOrigin(obj)}
        .collect(java.util.stream.Collectors.toUnmodifiableSet())
    }

    fun sanitizeStockOrigins(values: kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation>): kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation> {
        return values.stream().filter { obj: net.minecraft.resources.ResourceLocation -> OriginConfigCatalog.isStockOrigin(obj)}
        .collect(java.util.stream.Collectors.toUnmodifiableSet())
    }

    fun sanitizePowerMap(
    values: kotlin.collections.MutableMap<net.minecraft.resources.ResourceLocation, kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation>>?): kotlin.collections.MutableMap<net.minecraft.resources.ResourceLocation, kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation>> {
        if (values == null || values.isEmpty()) return java.util.Map.of<net.minecraft.resources.ResourceLocation, kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation>>()
        val out: kotlin.collections.MutableMap<net.minecraft.resources.ResourceLocation, kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation>> = LinkedHashMap<net.minecraft.resources.ResourceLocation, kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation>>()
        values.forEach { (origin: net.minecraft.resources.ResourceLocation, powers: kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation>) -> if (!OriginConfigCatalog.isAndromedaOrigin(origin)) return@forEach
        val clean: kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation> = LinkedHashSet<net.minecraft.resources.ResourceLocation>()
        for (power in powers) {
            val control: net.minecraft.resources.ResourceLocation = AndromedaConfigPolicy.controlPower(power)
            if (AndromedaConfigPolicy.isConfigurablePower(origin, control)) clean.add(control)
        }
        if (!clean.isEmpty()) out.put(origin, java.util.Set.copyOf<net.minecraft.resources.ResourceLocation>(clean))

        }
        return java.util.Map.copyOf<net.minecraft.resources.ResourceLocation, kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation>>(out)
    }


    fun andromeda(path: kotlin.String): net.minecraft.resources.ResourceLocation {
        return OriginConfigCatalog.andromeda(path)
    }
}
