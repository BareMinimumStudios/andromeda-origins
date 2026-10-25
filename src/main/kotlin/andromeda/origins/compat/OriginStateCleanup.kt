package andromeda.origins.compat

import andromeda.origins.AndromedaOrigins
import andromeda.origins.config.OriginConfigCatalog
import dev.overgrown.apoli.power.PowerContainer
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

/** Remove only an outgoing Origin's temporary grants, preserving other sources. */
object OriginStateCleanup {
    val ORIGIN_PREFIXES: Set<String> = OriginConfigCatalog.STANDARD.map { it.id.path }.toSet()
    val TRANSIENT_SOURCES: Set<ResourceLocation> = setOf(
        id("minecraft", "state"), id("minecraft", "debuff"), id("minecraft", "buff"),
        id("minecraft", "ccontrol"), id("minecraft", "helper"),
        id(AndromedaOrigins.MOD_ID, "nereid_mark"),
        id(AndromedaOrigins.MOD_ID, "nereid_submersion"),
        id(AndromedaOrigins.MOD_ID, "nereid_submersion_cast"),
        id(AndromedaOrigins.MOD_ID, "veil_step")
    )
    val SHARED_TRANSIENT_POWERS: Set<ResourceLocation> = setOf(
        id(AndromedaOrigins.CONTENT_NAMESPACE, "common/dash_projectile_penalty"),
        id(AndromedaOrigins.CONTENT_NAMESPACE, "common/undetectable")
    )

    fun cleanup(player: ServerPlayer?, originPrefix: String?): Int {
        if (player == null || originPrefix !in ORIGIN_PREFIXES) return 0
        if (originPrefix == "humanity") {
            if (MortalResolveManager.isActive(player)) MortalResolveManager.stop(player)
            if (MortalResolveManager.isChampionActive(player)) MortalResolveManager.stopChampion(player)
        }
        val container = PowerContainer.of(player) ?: return 0
        val standard = "$originPrefix/"
        val championNested = "champion/$originPrefix/"
        val championFlat = "champion/${originPrefix}_"
        var removed = 0
        for (power in container.allPowers().toList()) {
            if (power.namespace != AndromedaOrigins.CONTENT_NAMESPACE) continue
            if (!power.path.startsWith(standard) && !power.path.startsWith(championNested) &&
                !power.path.startsWith(championFlat) && power !in SHARED_TRANSIENT_POWERS) continue
            for (source in container.sourcesOf(power).toList()) {
                if (source in TRANSIENT_SOURCES && container.removePower(power, source)) removed++
            }
        }
        return removed
    }

    private fun id(namespace: String, path: String) = ResourceLocation.fromNamespaceAndPath(namespace, path)
}
