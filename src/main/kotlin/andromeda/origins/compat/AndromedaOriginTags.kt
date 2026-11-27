package andromeda.origins.compat

import dev.overgrown.origins.origin.Origin
import dev.overgrown.origins.origin.OriginRegistry
import dev.overgrown.origins.origin.OriginView
import net.minecraft.world.entity.player.Player




 /** Shared bridge for the same native Origin tags used by datapack conditions.  */
object AndromedaOriginTags {
    const val AQUATIC: String = "andromeda_origins:aquatic"
    const val CHAMPION: String = "andromeda_origins:champion"

     /** Mirrors origins:origin_tag's default MAIN selection semantics.  */
    fun hasChosenTag(player: Player?, tag: String?): Boolean {
        if (player == null || tag == null || tag.isEmpty()) return false
        for (originId in OriginView.chosen(player).values) {
            val origin: Origin? = OriginRegistry.get(originId)
            if (origin != null && origin.hasTag(tag)) return true
        }
        return false
    }
}
