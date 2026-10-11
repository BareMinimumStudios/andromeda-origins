package andromeda.origins.compat

import andromeda.origins.AndromedaOrigins
import andromeda.origins.config.OriginConfigCatalog
import andromeda.origins.config.OriginConfigCatalog.OriginEntry


 /**
 * Clears temporary helper state owned by an Andromeda Origin when that Origin is lost.
 *
 *
 * This is deliberately not a legacy-player migration layer. Overgrown owns the selected
 * Origin and its layer-sourced powers; this only removes short-lived Andromeda helper powers
 * that stable v1.4.75 granted from generic transient sources such as minecraft:state/buff/debuff.
 */
object OriginStateCleanup {
    val ORIGIN_PREFIXES: kotlin.collections.MutableSet<kotlin.String> = OriginConfigCatalog.STANDARD.stream()
    .map<kotlin.String> { entry: OriginEntry<*> -> entry.id.getPath()}
    .collect(java.util.stream.Collectors.toUnmodifiableSet())

    val TRANSIENT_SOURCES: kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation> = java.util.Set.of<net.minecraft.resources.ResourceLocation>(
    OriginStateCleanup.id("minecraft", "state"),
    OriginStateCleanup.id("minecraft", "debuff"),
    OriginStateCleanup.id("minecraft", "buff"),
    OriginStateCleanup.id("minecraft", "ccontrol"),
    OriginStateCleanup.id("minecraft", "helper"),
    OriginStateCleanup.id(AndromedaOrigins.MOD_ID, "nereid_mark"),
    OriginStateCleanup.id(AndromedaOrigins.MOD_ID, "nereid_submersion"),
    OriginStateCleanup.id(AndromedaOrigins.MOD_ID, "nereid_submersion_cast"),
    OriginStateCleanup.id(AndromedaOrigins.MOD_ID, "veil_step")
    )

     /**
 * Shared Andromeda helpers that are not stored under an Origin-specific path but still need
 * to be cleared when the owning Origin is removed. Keeping this allow-list explicit avoids
 * the old common/debug behavior, which revoked every minecraft:state power from every mod.
 */
    val SHARED_TRANSIENT_POWERS: kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation> = java.util.Set.of<net.minecraft.resources.ResourceLocation>(
    OriginStateCleanup.id(AndromedaOrigins.CONTENT_NAMESPACE, "common/dash_projectile_penalty"),
    OriginStateCleanup.id(AndromedaOrigins.CONTENT_NAMESPACE, "common/undetectable")
    )

    fun cleanup(player: net.minecraft.server.level.ServerPlayer?, originPrefix: kotlin.String?): kotlin.Int {
        if (player == null || !OriginStateCleanup.ORIGIN_PREFIXES.contains(originPrefix)) return 0
        val container: dev.overgrown.apoli.power.PowerContainer? = dev.overgrown.apoli.power.PowerContainer.of(player)
        if (container == null || container.isEmpty()) return 0

        val pathPrefix: kotlin.String = originPrefix + "/"
        var removed: kotlin.Int = 0
        for (powerId in java.util.List.copyOf<net.minecraft.resources.ResourceLocation>(container.allPowers())) {
            if (AndromedaOrigins.CONTENT_NAMESPACE != powerId.getNamespace()) continue
            val ownedByOrigin: kotlin.Boolean = powerId.getPath().startsWith(pathPrefix)
            val sharedTransient: kotlin.Boolean = OriginStateCleanup.SHARED_TRANSIENT_POWERS.contains(powerId)
            if (!ownedByOrigin && !sharedTransient) continue
            for (source in java.util.List.copyOf<net.minecraft.resources.ResourceLocation>(container.sourcesOf(powerId))) {
                if (!OriginStateCleanup.TRANSIENT_SOURCES.contains(source)) continue
                if (container.removePower(powerId, source)) removed++
            }
        }
        return removed
    }

    private fun id(namespace: kotlin.String, path: kotlin.String): net.minecraft.resources.ResourceLocation {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(namespace, path)
    }
}
