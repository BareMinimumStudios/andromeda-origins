package andromeda.origins.compat




 /** Shared bridge for the same native Origin tags used by datapack conditions.  */
object AndromedaOriginTags {
    const val AQUATIC: kotlin.String = "andromeda_origins:aquatic"
    const val CHAMPION: kotlin.String = "andromeda_origins:champion"

     /** Mirrors origins:origin_tag's default MAIN selection semantics.  */
    fun hasChosenTag(player: net.minecraft.world.entity.player.Player?, tag: kotlin.String?): kotlin.Boolean {
        if (player == null || tag == null || tag.isEmpty()) return false
        for (originId in dev.overgrown.origins.origin.OriginView.chosen(player).values) {
            val origin: dev.overgrown.origins.origin.Origin? = dev.overgrown.origins.origin.OriginRegistry.get(originId)
            if (origin != null && origin.hasTag(tag)) return true
        }
        return false
    }
}
