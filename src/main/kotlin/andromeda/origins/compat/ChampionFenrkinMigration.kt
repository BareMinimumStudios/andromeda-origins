package andromeda.origins.compat

import dev.overgrown.apoli.power.PowerContainer
import dev.overgrown.apoli.power.PowerResources
import dev.overgrown.origins.component.PlayerOriginsAttachment
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

/** One-time replacement of this Champion's old shared ability grants; foreign grants remain owned. */
object ChampionFenrkinMigration {
    private fun id(path: String) = ResourceLocation.fromNamespaceAndPath("andromeda_origins", path)
    fun reconcile(player: ServerPlayer) {
        val layer = ResourceLocation.parse("origins:origin")
        if (PlayerOriginsAttachment.get(player)?.getOrigin(layer) != id("champion_fenrkin")) return
        val c = PowerContainer.of(player) ?: return
        val owners = setOf(ResourceLocation.parse("origins:layer/origin"), id("champion_fenrkin"))
        for (ability in listOf("primary", "secondary")) {
            val old = id("fenrkin/$ability")
            val obsolete = c.sourcesOf(old).filter { it in owners }
            if (obsolete.isEmpty()) continue
            val next = id("champion/fenrkin_$ability")
            if (!c.hasPower(next)) continue
            val transfers = if (ability == "secondary") listOf("astamina" to 2.0)
                else listOf("active" to (2.0 / 3.0), "cooldown" to 0.4)
            val saved = transfers.mapNotNull { (child, scale) ->
                val from = id("fenrkin/${ability}_$child")
                if (!c.hasPower(from)) null else id("champion/fenrkin_${ability}_$child") to
                    kotlin.math.ceil(PowerResources.read(c, from).asInt * scale).toInt()
            }
            for (owner in obsolete) c.removePower(old, owner)
            for ((resource, value) in saved) PowerResources.write(c, resource, value)
        }
    }
}
